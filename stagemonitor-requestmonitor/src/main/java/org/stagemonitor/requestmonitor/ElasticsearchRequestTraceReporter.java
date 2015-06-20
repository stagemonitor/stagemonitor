package org.stagemonitor.requestmonitor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

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
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String path = String.format("/stagemonitor-%s/executions/%s", dateFormat.format(new Date()), requestTrace.getId());
		final String ttl = requestMonitorPlugin.getRequestTraceTtl();
		if (ttl != null && !ttl.isEmpty()) {
			path += "?ttl=" + ttl;
		}
		elasticsearchClient.sendAsJsonAsync("PUT", path, requestTrace);
	}

	@Override
	public boolean isActive() {
		return StringUtils.isNotEmpty(corePlugin.getElasticsearchUrl());
	}
}
