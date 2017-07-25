package org.stagemonitor.tracing.elasticsearch;

import com.fasterxml.jackson.core.JsonGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.ExecutorUtils;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.reporter.SpanReporter;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

/**
 * Holds spans in a {@link BlockingQueue} and flushes them asynchronously via a bulk request to Elasticsearch based on three conditions:
 * <ul>
 *     <li>Periodically, after the flush interval (defaults to 1 sec)</li>
 *     <li>When the span queue exceeds the max batch size and there is currently no flush scheduled, a immediate async flush is scheduled</li>
 *     <li>If the span queue size is still higher than the max batch size after a flush, spans are flushed again</li>
 * </ul>
 *
 * If the queue is full, spans are dropped to prevent excessive heap usage and {@link OutOfMemoryError}s
 */
public class ElasticsearchSpanReporter extends SpanReporter {

	static final MetricName spansDroppedMetricName = name("elasticsearch_spans_dropped").build();
	static final MetricName bulkSizeMetricName = name("elasticsearch_spans_bulk_size").build();
	static final String ES_SPAN_LOGGER = "ElasticsearchSpanReporter";
	private static final byte[] indexHeader = "{\"index\":{}}\n".getBytes(Charset.forName("UTF-8"));
	private static final String SPANS_TYPE = "spans";

	private final Logger spanLogger;

	private ElasticsearchTracingPlugin elasticsearchTracingPlugin;
	private ElasticsearchClient elasticsearchClient;
	private BlockingQueue<SpanWrapper> spansQueue;
	private TracingPlugin tracingPlugin;
	private ArrayList<SpanWrapper> spansBatch;
	private Metric2Registry metricRegistry;
	private ScheduledThreadPoolExecutor scheduler;

	public ElasticsearchSpanReporter() {
		this(LoggerFactory.getLogger(ES_SPAN_LOGGER));
	}

	ElasticsearchSpanReporter(Logger spanLogger) {
		this.spanLogger = spanLogger;
	}

	@Override
	public void init(ConfigurationRegistry configuration) {
		CorePlugin corePlugin = configuration.getConfig(CorePlugin.class);
		tracingPlugin = configuration.getConfig(TracingPlugin.class);
		elasticsearchTracingPlugin = configuration.getConfig(ElasticsearchTracingPlugin.class);
		elasticsearchClient = corePlugin.getElasticsearchClient();
		metricRegistry = corePlugin.getMetricRegistry();
		scheduler = ExecutorUtils.createSingleThreadSchedulingDeamonPool("elasticsearch-reporter", 10, corePlugin);
		scheduler.scheduleWithFixedDelay(new SpanFlushingRunnable(), elasticsearchTracingPlugin.getFlushDelayMs(),
				elasticsearchTracingPlugin.getFlushDelayMs(), TimeUnit.MILLISECONDS);
		spansBatch = new ArrayList<SpanWrapper>(elasticsearchTracingPlugin.getMaxBatchSize());
		spansQueue = new ArrayBlockingQueue<SpanWrapper>(elasticsearchTracingPlugin.getMaxQueueSize());
	}

	// executed in a single thread
	private boolean flush() {
		final int maxBatchSize = elasticsearchTracingPlugin.getMaxBatchSize();
		// the batching with drainTo should remove most of the contention imposed by the queue
		// as there is less contention on the head of the queue caused by elements being added and immediately removed
		spansQueue.drainTo(spansBatch, maxBatchSize);
		if (spansBatch.isEmpty()) {
			return false;
		}
		metricRegistry.histogram(bulkSizeMetricName).update(spansBatch.size());
		elasticsearchClient.sendBulk("/stagemonitor-spans-" + StringUtils.getLogstashStyleDate() + "/" + SPANS_TYPE, new HttpClient.OutputStreamHandler() {
			@Override
			public void withHttpURLConnection(OutputStream os) throws IOException {
				final JsonGenerator jsonGenerator = JsonUtils.getMapper().getFactory().createGenerator(os);
				for (SpanWrapper span : spansBatch) {
					writeSpanToOutputStream(jsonGenerator, os, span);
				}
				jsonGenerator.close();
				os.close();
			}
		});
		// reusing the batch list is safe as this method is executed single threaded
		spansBatch.clear();
		return spansQueue.size() >= maxBatchSize;
	}

	@Override
	public void report(SpanContextInformation spanContext, final SpanWrapper spanWrapper) {
		if (elasticsearchTracingPlugin.isOnlyLogElasticsearchSpanReports()) {
			final String spansIndex = "stagemonitor-spans-" + StringUtils.getLogstashStyleDate();
			spanLogger.info(ElasticsearchClient.getBulkHeader("index", spansIndex, SPANS_TYPE) + JsonUtils.toJson(spanWrapper));
		} else {
			final boolean addedToQueue = spansQueue.offer(spanWrapper);
			if (!addedToQueue) {
				metricRegistry.counter(spansDroppedMetricName).inc();
			}
			if (!tracingPlugin.isReportAsync()) {
				flush();
			} else {
				scheduleFlushIfSpanQueueExceedsMaxBatchSize();
			}
		}
	}

	private void scheduleFlushIfSpanQueueExceedsMaxBatchSize() {
		if (spansQueue.size() > elasticsearchTracingPlugin.getMaxBatchSize() && scheduler.getQueue().isEmpty()) {
			synchronized (this) {
				if (scheduler.getQueue().isEmpty()) {
					scheduler.schedule(new SpanFlushingRunnable(), 0, TimeUnit.SECONDS);
				}
			}
		}
	}

	private void writeSpanToOutputStream(JsonGenerator jsonGenerator, OutputStream outstream, SpanWrapper spanWrapper) throws IOException {
		outstream.write(indexHeader);
		jsonGenerator.writeObject(spanWrapper);
		outstream.write('\n');
	}

	@Override
	public boolean isActive(SpanContextInformation spanContext) {
		final boolean logOnly = elasticsearchTracingPlugin.isOnlyLogElasticsearchSpanReports();
		return elasticsearchClient.isElasticsearchAvailable() || logOnly;
	}

	private class SpanFlushingRunnable implements Runnable {
		@Override
		public void run() {
			try {
				boolean hasMoreElements = true;
				while (hasMoreElements) {
					hasMoreElements = flush();
				}
			} catch (Exception e) {
				spanLogger.warn("Exception while reporting spans to Elasticsearch", e);
			}
		}
	}
}
