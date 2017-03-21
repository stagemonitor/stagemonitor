package org.stagemonitor.vertx;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.ConfigurationOption;


import java.lang.instrument.Instrumentation;

public class VertxPlugin extends StagemonitorPlugin {
	public static final String VERTX_PLUGIN = "Vertx Plugin";

	private final ConfigurationOption<Boolean> useRxJava = ConfigurationOption.booleanOption()
			.key("stagemonitor.vertx.events.useRxJava")
			.dynamic(false)
			.label("Use rxjava for event handling")
			.description("Whether or not the app use rxjava for handling events")
			.defaultValue(false)
			.configurationCategory(VERTX_PLUGIN)
			.build();

	@Override
	public void initializePlugin(InitArguments initArguments) throws Exception {

	}

	public boolean isUseRxJava() {
		return useRxJava.getValue();
	}
}
