package org.stagemonitor.zipkin;

import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.core.StagemonitorPlugin;

public final class ZipkinPlugin extends StagemonitorPlugin {

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
			.key("stagemonitor.zipkin.reporter.messageTimeout")
			.dynamic(false)
			.label("Zipkin message timeout")
			.description("Instead of sending one message at a time, spans are bundled into messages. " +
					"Messages are sent when they fill the queue or after the timeout occurs, whichever happens first.")
			.tags("reporting", "zipkin")
			.configurationCategory(ZIPKIN_PLUGIN)
			.buildWithDefault(1000);
	private final ConfigurationOption<Integer> zipkinMaxQueuedBytes = ConfigurationOption.integerOption()
			.key("stagemonitor.zipkin.reporter.maxQueueSize")
			.dynamic(false)
			.label("Max zipkin queued bytes")
			.description("Maximum backlog of span bytes reported vs sent. Default 1% of heap. " +
					"The higher this value, the more memory overhead stagemonitor may impose to your application. " +
					"When the limit is reached, spans are dropped. To observe the amount of dropped spans, set " +
					"'stagemonitor.internal.monitoring' to true.")
			.tags("reporting", "zipkin")
			.configurationCategory(ZIPKIN_PLUGIN)
			.build();

	String getZipkinEndpoint() {
		return zipkinEndpoint.get();
	}

	int getZipkinFlushInterval() {
		return zipkinFlushInterval.get();
	}

	Integer getZipkinMaxQueuedBytes() {
		return zipkinMaxQueuedBytes.getValue();
	}
}
