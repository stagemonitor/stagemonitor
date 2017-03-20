package org.stagemonitor.zipkin;

import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.ConfigurationOption;

public class ZipkinPlugin extends StagemonitorPlugin {

	private static final String ZIPKIN_PLUGIN = "Zipkin Plugin";

	private final ConfigurationOption<String> zipkinEndpoint = ConfigurationOption.stringOption()
			.key("stagemonitor.zipkin.reporter.endpoint")
			.dynamic(false)
			.label("Zipkin endpoint")
			.description("The POST URL for zipkin's api (http://zipkin.io/zipkin-api/#/), " +
					"usually \"http://zipkinhost:9411/api/v1/spans\".")
			.tags("reporting", "zipkin")
			.configurationCategory(ZIPKIN_PLUGIN)
			.buildRequired();
	private final ConfigurationOption<Integer> zipkinFlushInterval = ConfigurationOption.integerOption()
			.key("stagemonitor.zipkin.reporter.flushInverval")
			.dynamic(false)
			.label("Zipkin flush interval (ms)")
			.tags("reporting", "zipkin")
			.configurationCategory(ZIPKIN_PLUGIN)
			.buildWithDefault(1000);
	private final ConfigurationOption<Integer> zipkinMaxQueueSize = ConfigurationOption.integerOption()
			.key("stagemonitor.zipkin.reporter.maxQueueSize")
			.dynamic(false)
			.label("Max zipkin queue size")
			.description("The max size of the queue which holds spans which should be reported to zipkin. " +
					"The higher this value, the more memory overhead stagemonitor may impose to your application. " +
					"When the limit is reached, spans are dropped. To observe the amount of dropped spans, set " +
					"'stagemonitor.internal.monitoring' to true.")
			.tags("reporting", "zipkin")
			.configurationCategory(ZIPKIN_PLUGIN)
			.buildWithDefault(1000);

	public String getZipkinEndpoint() {
		return zipkinEndpoint.get();
	}

	public int getZipkinFlushInterval() {
		return zipkinFlushInterval.get();
	}

	public int getZipkinMaxQueueSize() {
		return zipkinMaxQueueSize.get();
	}

}
