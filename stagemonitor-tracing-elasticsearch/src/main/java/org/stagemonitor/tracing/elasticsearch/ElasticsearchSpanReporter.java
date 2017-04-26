package org.stagemonitor.tracing.elasticsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.reporter.SpanReporter;
import org.stagemonitor.util.StringUtils;

public class ElasticsearchSpanReporter extends SpanReporter {

	public static final String ES_SPAN_LOGGER = "ElasticsearchSpanReporter";

	private final Logger spanLogger;

	protected CorePlugin corePlugin;
	protected ElasticsearchTracingPlugin elasticsearchTracingPlugin;
	protected ElasticsearchClient elasticsearchClient;
	private static final String SPANS_TYPE = "spans";

	public ElasticsearchSpanReporter() {
		this(LoggerFactory.getLogger(ES_SPAN_LOGGER));
	}

	ElasticsearchSpanReporter(Logger spanLogger) {
		this.spanLogger = spanLogger;
	}

	@Override
	public void init(ConfigurationRegistry configuration) {
		corePlugin = configuration.getConfig(CorePlugin.class);
		elasticsearchTracingPlugin = configuration.getConfig(ElasticsearchTracingPlugin.class);
		elasticsearchClient = corePlugin.getElasticsearchClient();
	}

	@Override
	public void report(SpanContextInformation spanContext) {
		final String spansIndex = "stagemonitor-spans-" + StringUtils.getLogstashStyleDate();
		if (elasticsearchTracingPlugin.isOnlyLogElasticsearchSpanReports()) {
			spanLogger.info(ElasticsearchClient.getBulkHeader("index", spansIndex, SPANS_TYPE) + JsonUtils.toJson(spanContext.getReadbackSpan()));
		} else {
			elasticsearchClient.index(spansIndex, SPANS_TYPE, spanContext.getReadbackSpan());
		}
	}

	@Override
	public boolean isActive(SpanContextInformation spanContext) {
		final boolean logOnly = elasticsearchTracingPlugin.isOnlyLogElasticsearchSpanReports();
		return elasticsearchClient.isElasticsearchAvailable() || logOnly;
	}

}
