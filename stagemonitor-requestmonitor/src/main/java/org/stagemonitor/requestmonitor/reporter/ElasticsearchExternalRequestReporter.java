package org.stagemonitor.requestmonitor.reporter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.ExternalRequest;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

/**
 * An implementation of {@link RequestTraceReporter} that reports
 * {@link org.stagemonitor.requestmonitor.RequestTrace#externalRequests} into to <code>stagemonitor-external-requests-*</code>
 * Elasticsearch index
 */
public class ElasticsearchExternalRequestReporter extends RequestTraceReporter {
	public static final String ES_EXTERNAL_REQUEST_TRACE_LOGGER = "ElasticsearchExternalRequestTraces";
	private static final byte[] BULK_HEADER = "{\"index\":{}}\n".getBytes(Charset.forName("UTF-8"));
	private CorePlugin corePlugin;
	private RequestMonitorPlugin requestMonitorPlugin;
	private ElasticsearchClient elasticsearchClient;
	private final Logger requestTraceLogger;

	public ElasticsearchExternalRequestReporter() {
		this(LoggerFactory.getLogger(ES_EXTERNAL_REQUEST_TRACE_LOGGER));
	}

	public ElasticsearchExternalRequestReporter(Logger requestTraceLogger) {
		this.requestTraceLogger = requestTraceLogger;
	}

	@Override
	public void init(InitArguments initArguments) {
		final Configuration configuration = initArguments.getConfiguration();
		corePlugin = configuration.getConfig(CorePlugin.class);
		requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
		elasticsearchClient = corePlugin.getElasticsearchClient();
	}

	@Override
	public void reportRequestTrace(final ReportArguments reportArguments) throws Exception {
		final String index = "stagemonitor-external-requests-" + StringUtils.getLogstashStyleDate();
		if (!requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()) {
			elasticsearchClient.sendBulkAsync("/" + index + "/requests", new HttpClient.OutputStreamHandler() {
				@Override
				public void withHttpURLConnection(OutputStream os) throws IOException {
					writeExternalRequestsToOutputStream(os, reportArguments.getRequestTrace().getExternalRequests());
				}
			});
		} else {
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			writeExternalRequestsToOutputStream(os, reportArguments.getRequestTrace().getExternalRequests());
			requestTraceLogger.info(new String(os.toByteArray(), Charset.forName("UTF-8")));
		}
	}

	private void writeExternalRequestsToOutputStream(OutputStream os, Collection<ExternalRequest> externalRequests) throws IOException {
		for (ExternalRequest externalRequest : externalRequests) {
			os.write(BULK_HEADER);
			JsonUtils.writeJsonToOutputStream(externalRequest, os);
			os.write('\n');
		}
		os.close();
	}

	@Override
	public boolean isActive(IsActiveArguments isActiveArguments) {
		final boolean urlAvailable = !corePlugin.getElasticsearchUrls().isEmpty();
		final boolean logOnly = requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports();
		return (urlAvailable || logOnly);
	}
}
