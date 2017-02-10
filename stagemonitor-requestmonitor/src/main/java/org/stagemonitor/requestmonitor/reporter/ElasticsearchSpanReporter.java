package org.stagemonitor.requestmonitor.reporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

public class ElasticsearchSpanReporter extends SpanReporter {

	public static final String ES_SPAN_LOGGER = "ElasticsearchSpanReporter";

	private final Logger spanLogger;

	protected CorePlugin corePlugin;
	protected RequestMonitorPlugin requestMonitorPlugin;
	protected ElasticsearchClient elasticsearchClient;
	private static final String SPANS_TYPE = "spans";

	public ElasticsearchSpanReporter() {
		this(LoggerFactory.getLogger(ES_SPAN_LOGGER));
	}

	ElasticsearchSpanReporter(Logger spanLogger) {
		this.spanLogger = spanLogger;
	}

	@Override
	public void init(InitArguments initArguments) {
		corePlugin = initArguments.getConfiguration().getConfig(CorePlugin.class);
		requestMonitorPlugin = initArguments.getConfiguration().getConfig(RequestMonitorPlugin.class);
		elasticsearchClient = corePlugin.getElasticsearchClient();
	}

	@Override
	public void report(RequestMonitor.RequestInformation requestInformation) {
		final String spansIndex = "stagemonitor-spans-" + StringUtils.getLogstashStyleDate();
		if (requestMonitorPlugin.isOnlyLogElasticsearchSpanReports()) {
			spanLogger.info(ElasticsearchClient.getBulkHeader("index", spansIndex, SPANS_TYPE) + JsonUtils.toJson(requestInformation.getSpan()));
		} else {
			elasticsearchClient.index(spansIndex, SPANS_TYPE, requestInformation.getSpan());
		}
	}

	@Override
	public boolean isActive(RequestMonitor.RequestInformation requestInformation) {
		final boolean logOnly = requestMonitorPlugin.isOnlyLogElasticsearchSpanReports();
		return (elasticsearchClient.isElasticsearchAvailable() || logOnly);
	}

}
