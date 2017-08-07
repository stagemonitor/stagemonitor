package org.stagemonitor.tracing.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.ExecutorUtils;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.tracing.B3HeaderFormat;
import org.stagemonitor.tracing.TracingPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class ElasticsearchUpdateSpanReporter {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchUpdateSpanReporter.class);
	private static final byte[] EMPTY_QUERY_HEADER = "{}\n".getBytes(ElasticsearchSpanReporter.UTF_8);
	private static final byte[] QUERY_PART1 = "{\"query\": {\"bool\": {\"must\": [{\"term\": {\"id\": {\"value\": \"".getBytes(ElasticsearchSpanReporter.UTF_8);
	private static final byte[] QUERY_PART2 = "\"}}},{\"term\": {\"trace_id\":{\"value\": \"".getBytes(ElasticsearchSpanReporter.UTF_8);
	private static final byte[] QUERY_PART3 = "\"}}},{\"range\": {\"@timestamp\": {\"gte\": \"now-1h\"}}}]}},\"_source\": \"trace_id\"}\n".getBytes(ElasticsearchSpanReporter.UTF_8);
	/**
	 * The time after which {@link UpdateDescription}s are discarded when no Elasticsearch id could be determined (via
	 * _msearch).
	 */
	private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);
	private static final long ES_REFRESH_INTERVAL = TimeUnit.SECONDS.toMillis(5);
	private final JsonNodeResponseHandler responseHandler = new JsonNodeResponseHandler();
	private final MultiSearchOutputStreamHandler multiSearchOutputStreamHandler = new MultiSearchOutputStreamHandler();
	private final Runnable updateRunnable;
	private final ElasticsearchTracingPlugin elasticsearchTracingPlugin;

	private final BlockingQueue<UpdateDescription> updateDescriptionQueue;
	private final List<UpdateDescription> updateBatch;
	private final ElasticsearchSpanReporter elasticsearchSpanReporter;
	private final CorePlugin corePlugin;
	private final ElasticsearchClient elasticsearchClient;
	private final HttpClient httpClient;
	private final TracingPlugin tracingPlugin;

	public ElasticsearchUpdateSpanReporter(CorePlugin corePlugin, TracingPlugin tracingPlugin,
										   ElasticsearchTracingPlugin elasticsearchTracingPlugin,
										   ElasticsearchSpanReporter elasticsearchSpanReporter) {
		this.tracingPlugin = tracingPlugin;
		this.corePlugin = corePlugin;
		this.elasticsearchClient = corePlugin.getElasticsearchClient();
		this.elasticsearchSpanReporter = elasticsearchSpanReporter;
		this.httpClient = elasticsearchClient.getHttpClient();
		this.elasticsearchTracingPlugin = elasticsearchTracingPlugin;

		updateBatch = new ArrayList<UpdateDescription>(elasticsearchTracingPlugin.getMaxBatchSize());
		updateDescriptionQueue = new ArrayBlockingQueue<UpdateDescription>(1000);
		updateRunnable = new SpanFlushingRunnable(new UpdateCallable());
		final ScheduledThreadPoolExecutor scheduler = ExecutorUtils.createSingleThreadSchedulingDeamonPool("elasticsearch-update-reporter", 10, corePlugin);
		scheduler.scheduleWithFixedDelay(updateRunnable, elasticsearchTracingPlugin.getFlushDelayMs(), elasticsearchTracingPlugin.getFlushDelayMs(), TimeUnit.MILLISECONDS);
	}

	public void updateSpan(B3HeaderFormat.B3Identifiers spanIdentifiers, Map<String, Object> tagsToUpdate) {
		if (!elasticsearchClient.isElasticsearchAvailable()) {
			return;
		}
		final UpdateDescription updateDescription = new UpdateDescription();
		updateDescription.spanIdentifiers = spanIdentifiers;
		final long now = System.currentTimeMillis();
		if (tracingPlugin.isReportAsync()) {
			final long delay = elasticsearchTracingPlugin.getFlushDelayMs() + ES_REFRESH_INTERVAL;
			updateDescription.tryToFindIdNotBefore = now + delay;
		} else {
			updateDescription.tryToFindIdNotBefore = now;
		}
		updateDescription.tryToFindIdNotAfter = now + TIMEOUT;
		updateDescription.tagsToUpdate = tagsToUpdate;
		addToUpdateDescriptionQueue(updateDescription);
		logger.debug("scheduling update for span {}", spanIdentifiers);
		if (!tracingPlugin.isReportAsync()) {
			updateRunnable.run();
		}
	}

	void flush() {
		while (!updateDescriptionQueue.isEmpty()) {
			updateRunnable.run();
		}
	}

	private static class UpdateDescription {
		private String id;
		// don't try to get the id before this timestamp as the span probably is not available yet
		private long tryToFindIdNotBefore;
		private long tryToFindIdNotAfter;
		private B3HeaderFormat.B3Identifiers spanIdentifiers;
		private Map<String, Object> tagsToUpdate;
	}

	static class BulkUpdateOutputStreamHandler implements HttpClient.OutputStreamHandler {
		private final String id;
		private final Object partialDocument;

		BulkUpdateOutputStreamHandler(String id, Object partialDocument) {
			this.id = id;
			this.partialDocument = partialDocument;
		}

		@Override
		public void withHttpURLConnection(OutputStream os) throws IOException {
			os.write("{\"update\":{\"_id\":\"".getBytes(ElasticsearchSpanReporter.UTF_8));
			os.write(id.getBytes(ElasticsearchSpanReporter.UTF_8));
			os.write("\"}}\n".getBytes(ElasticsearchSpanReporter.UTF_8));
			os.write("{\"doc\":".getBytes(ElasticsearchSpanReporter.UTF_8));
			JsonUtils.writeWithoutClosingStream(os, partialDocument);
			os.write('}');
			os.write('\n');
		}
	}

	private class UpdateCallable implements Callable<Boolean> {

		private final Map<Object, UpdateDescription> updateDescriptionByTraceId = new HashMap<Object, UpdateDescription>();

		@Override
		public Boolean call() throws Exception {
			try {
				updateDescriptionQueue.drainTo(updateBatch);
				removeUpdateDescriptionsWhichShouldNotRunYet();
				if (updateBatch.isEmpty()) {
					return false;
				}
				logger.debug("Performing id search for {} spans", updateBatch.size());
				initLookupMap();
				JsonNode multiSearchResponse = searchForElasticsearchIdsByTraceIds();
				setElasticsearchIds(multiSearchResponse, updateDescriptionByTraceId);
				scheduleUpdateOrPutBackToQueue();
				return updateDescriptionQueue.size() >= elasticsearchTracingPlugin.getMaxBatchSize();
			} finally {
				updateBatch.clear();
				updateDescriptionByTraceId.clear();
			}
		}

		private void initLookupMap() {
			for (UpdateDescription updateDescription : updateBatch) {
				updateDescriptionByTraceId.put(updateDescription.spanIdentifiers.getTraceId(), updateDescription);
			}
		}

		private void removeUpdateDescriptionsWhichShouldNotRunYet() {
			final long now = System.currentTimeMillis();
			for (Iterator<UpdateDescription> iterator = updateBatch.iterator(); iterator.hasNext(); ) {
				UpdateDescription updateDescription = iterator.next();
				if (now < updateDescription.tryToFindIdNotBefore) {
					iterator.remove();
					addToUpdateDescriptionQueue(updateDescription);
				}
			}
		}

		private JsonNode searchForElasticsearchIdsByTraceIds() {
			return httpClient.send("POST",
					corePlugin.getElasticsearchUrl() + "/stagemonitor-spans*/_msearch",
					ElasticsearchClient.CONTENT_TYPE_NDJSON,
					multiSearchOutputStreamHandler,
					responseHandler);
		}

		private void scheduleUpdateOrPutBackToQueue() {
			final long now = System.currentTimeMillis();
			for (UpdateDescription updateDescription : updateBatch) {
				if (updateDescription.id == null) {
					if (updateDescription.tryToFindIdNotAfter > now) {
						// not found; maybe not yet indexed or refreshed (available for search)
						logger.debug("Could not find id for span {}. Putting back in queue and try again later", updateDescription.spanIdentifiers, TIMEOUT);
						addToUpdateDescriptionQueue(updateDescription);
					} else {
						logger.debug("Discarding update span {} because it could not be found within {}ms", updateDescription.spanIdentifiers, TIMEOUT);
					}
				} else {
					logger.debug("Scheduling update for span {}", updateDescription.spanIdentifiers);
					elasticsearchSpanReporter.scheduleSendBulk(new BulkUpdateOutputStreamHandler(updateDescription.id, updateDescription.tagsToUpdate));
				}
			}
		}

		private void setElasticsearchIds(JsonNode multiSearchResponse, Map<Object, UpdateDescription> updateDescriptionByTraceId) {
			for (JsonNode response : multiSearchResponse.get("responses")) {
				final JsonNode hits = response.get("hits");
				if (hits == null) {
					if (response.get("error") != null) {
						logger.debug("{}", response);
					}
					continue;
				}
				final JsonNode innerHits = hits.get("hits");
				if (innerHits != null && innerHits.size() == 1) {
					JsonNode hit = innerHits.get(0);
					final String traceId = hit.get("_source").get("trace_id").asText();
					final UpdateDescription updateDescription = updateDescriptionByTraceId.get(traceId);
					updateDescription.id = hit.get("_id").asText();
					logger.debug("Elasticsearch id for span {} is {}", updateDescription.spanIdentifiers, updateDescription.id);
				}
			}
		}
	}

	private void addToUpdateDescriptionQueue(UpdateDescription updateDescription) {
		final boolean addedToQueue = updateDescriptionQueue.offer(updateDescription);
		if (!addedToQueue) {
			logger.debug("Could not add updateDescription to queue because it is full");
		}
	}

	private class MultiSearchOutputStreamHandler implements HttpClient.OutputStreamHandler {

		@Override
		public void withHttpURLConnection(OutputStream os) throws IOException {
			for (UpdateDescription updateDescription : updateBatch) {
				writeMultiSearchEntry(os, updateDescription);
			}
			os.close();
		}

		private void writeMultiSearchEntry(OutputStream os, UpdateDescription updateDescription) throws IOException {
			os.write(EMPTY_QUERY_HEADER);
			os.write(QUERY_PART1);
			os.write(updateDescription.spanIdentifiers.getSpanId().getBytes(ElasticsearchSpanReporter.UTF_8));
			os.write(QUERY_PART2);
			os.write(updateDescription.spanIdentifiers.getTraceId().getBytes(ElasticsearchSpanReporter.UTF_8));
			os.write(QUERY_PART3);
		}
	}

	private static class JsonNodeResponseHandler implements HttpClient.ResponseHandler<JsonNode> {
		private final ElasticsearchClient.BulkErrorReportingResponseHandler errorHandler = new ElasticsearchClient.BulkErrorReportingResponseHandler();

		@Override
		public JsonNode handleResponse(InputStream is, Integer statusCode, IOException e) throws IOException {
			if (statusCode == 200) {
				return JsonUtils.getMapper().readTree(is);
			} else {
				errorHandler.handleResponse(is, statusCode, e);
				return null;
			}
		}
	}
}
