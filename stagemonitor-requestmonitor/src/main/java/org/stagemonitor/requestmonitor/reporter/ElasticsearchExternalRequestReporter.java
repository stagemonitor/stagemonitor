package org.stagemonitor.requestmonitor.reporter;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.MetricUtils;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.ExternalRequest;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

/**
 * An implementation of {@link RequestTraceReporter} that reports
 * {@link org.stagemonitor.requestmonitor.RequestTrace#externalRequests} into to <code>stagemonitor-external-requests-*</code>
 * Elasticsearch index
 */
public class ElasticsearchExternalRequestReporter extends RequestTraceReporter {
	private static final String ES_EXTERNAL_REQUEST_TRACE_LOGGER = "ElasticsearchExternalRequestTraces";
	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchExternalRequestReporter.class);
	private final Logger externalRequestsLogger;

	private static final byte[] BULK_HEADER = "{\"index\":{}}\n".getBytes(Charset.forName("UTF-8"));
	private CorePlugin corePlugin;
	private RequestMonitorPlugin requestMonitorPlugin;
	private ElasticsearchClient elasticsearchClient;
	private Meter reportingRate = new Meter();

	public ElasticsearchExternalRequestReporter() {
		this(LoggerFactory.getLogger(ES_EXTERNAL_REQUEST_TRACE_LOGGER));
	}

	public ElasticsearchExternalRequestReporter(Logger externalRequestsLogger) {
		this.externalRequestsLogger = externalRequestsLogger;
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
		final List<ExternalRequest> externalRequests = reportArguments.getRequestTrace().getExternalRequests();
		for (Iterator<ExternalRequest> iterator = externalRequests.iterator(); iterator.hasNext(); ) {
			final ExternalRequest externalRequest = iterator.next();
			trackExternalRequestMetrics(externalRequest);
			if (false && !isReportExternalRequest(externalRequest)) {
				iterator.remove();
			}
		}

		if (!externalRequests.isEmpty() && isReportToElasticsearch()) {
			reportExternalRequestsToElasticsearch(externalRequests);
		}
	}

	private void reportExternalRequestsToElasticsearch(final List<ExternalRequest> externalRequests) throws IOException {
		if (requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()) {
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			writeExternalRequestsToOutputStream(os, externalRequests);
			externalRequestsLogger.info(new String(os.toByteArray(), Charset.forName("UTF-8")));
		} else if (!corePlugin.getElasticsearchUrls().isEmpty()) {
			final String index = "stagemonitor-external-requests-" + StringUtils.getLogstashStyleDate();
			elasticsearchClient.sendBulkAsync("/" + index + "/requests", new HttpClient.OutputStreamHandler() {
				@Override
				public void withHttpURLConnection(OutputStream os) throws IOException {
					writeExternalRequestsToOutputStream(os, externalRequests);
				}
			});
		}
	}

	private void trackExternalRequestMetrics(ExternalRequest externalRequest) {
		// 0 means the time could not be determined
		if (externalRequest.getExecutionTimeNanos() <= 0) {
			return;
		}
		final long duration = externalRequest.getExecutionTimeNanos();
		corePlugin.getMetricRegistry()
				.timer(getExternalRequestTimerName(externalRequest, "All"))
				.update(duration, TimeUnit.NANOSECONDS);
		if (externalRequest.getExecutedBy() != null) {
			corePlugin.getMetricRegistry()
					.timer(getExternalRequestTimerName(externalRequest))
					.update(duration, TimeUnit.NANOSECONDS);
		}
	}

	public static MetricName getExternalRequestTimerName(ExternalRequest externalRequest) {
		return getExternalRequestTimerName(externalRequest, externalRequest.getExecutedBy());
	}

	public static MetricName getExternalRequestTimerName(ExternalRequest externalRequest, String signature) {
		return name("external_request_response_time")
				.type(externalRequest.getRequestType())
				.tag("signature", signature)
				.tag("method", externalRequest.getRequestMethod()).build();
	}

	private void writeExternalRequestsToOutputStream(OutputStream os, Collection<ExternalRequest> externalRequests) throws IOException {
		for (ExternalRequest externalRequest : externalRequests) {
			os.write(BULK_HEADER);
			os.write(JsonUtils.getMapper().writeValueAsBytes(externalRequest));
			os.write('\n');
			reportingRate.mark();
		}
		os.write('\n');
		os.close();
	}

	private boolean isReportExternalRequest(ExternalRequest externalRequest) {
		if (externalRequest.getExecutionTime() < requestMonitorPlugin.getExcludeExternalRequestsFasterThan()) {
			logger.debug("Exclude external request {} because it was faster than {}", externalRequest.getExecutedBy(),
					requestMonitorPlugin.getExcludeExternalRequestsFasterThan());
			return false;
		}
		if (MetricUtils.isRateLimitExceeded(requestMonitorPlugin.getOnlyReportNExternalRequestsPerMinute(), reportingRate)) {
			logger.debug("Exclude external request {} because would exceed the reporting rate of {}",
					externalRequest.getExecutedBy(), requestMonitorPlugin.getOnlyReportNExternalRequestsPerMinute());
			return false;
		}
		Timer timer = corePlugin.getMetricRegistry().timer(getExternalRequestTimerName(externalRequest));
		final double percentageThreshold = requestMonitorPlugin.getExcludeExternalRequestsWhenFasterThanXPercent();
		if (!MetricUtils.isFasterThanXPercentOfAllRequests(externalRequest.getExecutionTimeNanos(), percentageThreshold, timer)) {
			logger.debug("Exclude external request {} because was faster than {}% of all requests",
					externalRequest.getExecutedBy(), percentageThreshold * 100);
			return false;
		}
		return true;
	}

	private boolean isReportToElasticsearch() {
		final boolean urlAvailable = !corePlugin.getElasticsearchUrls().isEmpty();
		final boolean logOnly = requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports();
		return (urlAvailable || logOnly);
	}

	@Override
	public boolean isActive(IsActiveArguments isActiveArguments) {
		return true;
	}

	@Override
	public boolean requiresCallTree() {
		return false;
	}
}
