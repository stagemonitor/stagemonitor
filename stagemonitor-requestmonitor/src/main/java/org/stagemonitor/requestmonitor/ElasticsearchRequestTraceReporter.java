package org.stagemonitor.requestmonitor;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StageMonitor;
import org.stagemonitor.core.rest.RestClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * An implementation of {@link RequestTraceReporter} that sends the {@link RequestTrace} to Elasticsearch
 */
public class ElasticsearchRequestTraceReporter implements RequestTraceReporter {

	private final CorePlugin corePlugin;
	private final RequestMonitorPlugin requestMonitorPlugin;

	public ElasticsearchRequestTraceReporter() {
		this(StageMonitor.getConfiguration(CorePlugin.class), StageMonitor.getConfiguration(RequestMonitorPlugin.class));
	}

	public ElasticsearchRequestTraceReporter(CorePlugin corePlugin, RequestMonitorPlugin requestMonitorPlugin) {
		this.corePlugin = corePlugin;
		this.requestMonitorPlugin = requestMonitorPlugin;
	}

	@Override
	public <T extends RequestTrace> void reportRequestTrace(T requestTrace) {
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String path = String.format("/stagemonitor-%s/executions/%s", dateFormat.format(new Date()), requestTrace.getId());
		final String ttl = requestMonitorPlugin.getRequestTraceTtl();
		if (ttl != null && !ttl.isEmpty()) {
			path += "?ttl=" + ttl;
		}
		RestClient.sendAsJsonAsync(corePlugin.getElasticsearchUrl(), path, "PUT", requestTrace);
	}

	@Override
	public boolean isActive() {
		final String elasticsearchUrl = corePlugin.getElasticsearchUrl();
		return elasticsearchUrl != null && !elasticsearchUrl.isEmpty();
	}
}
