package org.stagemonitor.logging;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Collections;
import java.util.List;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.agent.StagemonitorAgent;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class LoggingPlugin extends StagemonitorPlugin {

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
		retransformLogger();
		ElasticsearchClient elasticsearchClient = configuration.getConfig(CorePlugin.class).getElasticsearchClient();
		elasticsearchClient.sendGrafanaDashboardAsync("Logging.json");
	}

	@Override
	public List<String> getPathsOfWidgetMetricTabPlugins() {
		return Collections.singletonList("/stagemonitor/static/tabs/metrics/logging-metrics");
	}

	public void retransformLogger() {
		final Instrumentation instrumentation = StagemonitorAgent.getInstrumentation();
		if (instrumentation != null && instrumentation.isRetransformClassesSupported()) {
			try {
				for (Class<?> clazz : MeterLoggingInstrumenter.getClassesToRetransform(instrumentation)) {
					instrumentation.retransformClasses(clazz);
				}
			} catch (UnmodifiableClassException e) {
				e.printStackTrace();
			}
		}
	}
}
