package org.stagemonitor.requestmonitor;

import java.util.Collection;

import com.codahale.metrics.Meter;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.StringUtils;

/**
 * An implementation of {@link RequestTraceReporter} that sends the {@link RequestTrace} to Elasticsearch
 */
public class ElasticsearchRequestTraceReporter implements RequestTraceReporter {

	private final CorePlugin corePlugin;
	private final RequestMonitorPlugin requestMonitorPlugin;
	private final ElasticsearchClient elasticsearchClient;
	private final Meter reportingRate = new Meter();

	public ElasticsearchRequestTraceReporter() {
		this(Stagemonitor.getConfiguration(CorePlugin.class), Stagemonitor.getConfiguration(RequestMonitorPlugin.class),
				Stagemonitor.getConfiguration().getConfig(CorePlugin.class).getElasticsearchClient());
	}

	public ElasticsearchRequestTraceReporter(CorePlugin corePlugin, RequestMonitorPlugin requestMonitorPlugin,
											 ElasticsearchClient elasticsearchClient) {
		this.corePlugin = corePlugin;
		this.requestMonitorPlugin = requestMonitorPlugin;
		this.elasticsearchClient = elasticsearchClient;
	}

	@Override
	public <T extends RequestTrace> void reportRequestTrace(T requestTrace) {
		final String requestTraceName = requestTrace.getName();
		if (isReportRequestTraceName(requestTraceName) && !isReportingRateExceeded()) {
			reportingRate.mark();
			elasticsearchClient.index("stagemonitor-requests-" + StringUtils.getLogstashStyleDate(), "requests", requestTrace);
		}
	}

	private boolean isReportRequestTraceName(String requestTraceName) {
		final Collection<String> onlyReportRequestsWithName = requestMonitorPlugin.getOnlyReportRequestsWithNameToElasticsearch();
		return onlyReportRequestsWithName.isEmpty() || onlyReportRequestsWithName.contains(requestTraceName);
	}

	private boolean isReportingRateExceeded() {
		final int maxReportingRate = requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch();
		if (Integer.MAX_VALUE == maxReportingRate) {
			return false;
		} else if (maxReportingRate <= 0) {
			return true;
		}
		final double v = 60 * reportingRate.getOneMinuteRate();
		return v > maxReportingRate;
	}

	@Override
	public <T extends RequestTrace> boolean isActive(T requestTrace) {
		return StringUtils.isNotEmpty(corePlugin.getElasticsearchUrl());
	}
}
