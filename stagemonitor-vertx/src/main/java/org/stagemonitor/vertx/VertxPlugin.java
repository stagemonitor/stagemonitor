package org.stagemonitor.vertx;

import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.vertx.utils.RequestNamer;

public class VertxPlugin extends StagemonitorPlugin {
	public static final String VERTX_PLUGIN = "Vertx Plugin";

	private final ConfigurationOption<String> requestNamerImplementation = ConfigurationOption.stringOption()
			.key("stagemonitor.vertx.eventbus.requestNamerImplementation")
			.dynamic(true)
			.label("The class name of your implementation of RequestNamer")
			.description("The default implementation name the request with the address of the message. "
					+ "If you want a different naming, implement the RequestNamer interface. "
					+ "The class that implements it must have a default unparameterized constructor")
			.configurationCategory(VERTX_PLUGIN)
			.buildWithDefault("org.stagemonitor.vertx.utils.DefaultRequestNamer");

	private final ConfigurationOption<String> eventBusImplementation = ConfigurationOption.stringOption()
			.key("stagemonitor.vertx.eventbus.eventBusImplementation")
			.dynamic(true)
			.label("Class name of EventBus implementation")
			.description("If you don't use vertx default implementation of the EventBus provide your implementation here.")
			.configurationCategory(VERTX_PLUGIN)
			.buildWithDefault("io.vertx.core.eventbus.impl.EventBusImpl");

	private final ConfigurationOption<String> messageConsumerImplementation = ConfigurationOption.stringOption()
			.key("stagemonitor.vertx.eventbus.messageConsumerImplementation")
			.dynamic(true)
			.label("Class name of MessageConsumer implementation")
			.description("If you don't use vertx default implementation of the MessageConsumer provide your implementation here.")
			.configurationCategory(VERTX_PLUGIN)
			.buildWithDefault("io.vertx.core.eventbus.impl.EventBusImpl$HandlerRegistration");

	private final ConfigurationOption<String> webRouteImplementation = ConfigurationOption.stringOption()
			.key("stagemonitor.vertx.web.webRouteImplementation")
			.dynamic(true)
			.label("Class name of Route implementation of vertx-web")
			.description("If you don't use vertx default implementation of the Route provide your implementation here.")
			.configurationCategory(VERTX_PLUGIN)
			.buildWithDefault("io.vertx.ext.web.impl.RouteImpl");

	@Override
	public void initializePlugin(InitArguments initArguments) throws Exception {

	}

	public RequestNamer getRequestNamer() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		return (RequestNamer) Class.forName(requestNamerImplementation.getValue()).newInstance();
	}

	public String getEventBusImplementation() {
		return eventBusImplementation.getValue();
	}

	public String getMessageConsumerImplementation() {
		return messageConsumerImplementation.getValue();
	}

	public String getWebRouteImplementation() {
		return webRouteImplementation.getValue();
	}
}
