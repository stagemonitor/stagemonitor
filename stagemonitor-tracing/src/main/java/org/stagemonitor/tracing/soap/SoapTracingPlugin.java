package org.stagemonitor.tracing.soap;

import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.tracing.TracingPlugin;

import java.util.Collections;
import java.util.List;

public class SoapTracingPlugin extends StagemonitorPlugin {

	private static final String SOAP_TRACING_PLUGIN = "SOAP Tracing Plugin";

	private final ConfigurationOption<Boolean> soapClientRecordRequestMessages = ConfigurationOption.booleanOption()
			.key("stagemonitor.tracing.soap.client.recordRequestMessages")
			.dynamic(true)
			.label("Record SOAP client request messages")
			.description("When set to true, the SOAP message of client SOAP requests will be attached to the span.")
			.configurationCategory(SOAP_TRACING_PLUGIN)
			.buildWithDefault(false);
	private final ConfigurationOption<Boolean> soapClientRecordResponseMessages = ConfigurationOption.booleanOption()
			.key("stagemonitor.tracing.soap.client.recordResponseMessages")
			.dynamic(true)
			.label("Record SOAP client response messages")
			.description("When set to true, the SOAP message of SOAP responses received by SOAP clients will be attached to the span.")
			.configurationCategory(SOAP_TRACING_PLUGIN)
			.buildWithDefault(false);
	private final ConfigurationOption<Boolean> soapServerRecordRequestMessages = ConfigurationOption.booleanOption()
			.key("stagemonitor.tracing.soap.server.recordRequestMessages")
			.dynamic(true)
			.label("Record SOAP incoming request messages")
			.description("When set to true, the SOAP message of incoming SOAP requests will be attached to the span.")
			.configurationCategory(SOAP_TRACING_PLUGIN)
			.buildWithDefault(false);
	private final ConfigurationOption<Boolean> soapServerRecordResponseMessages = ConfigurationOption.booleanOption()
			.key("stagemonitor.tracing.soap.server.recordResponseMessages")
			.dynamic(true)
			.label("Record SOAP outgoing response messages")
			.description("When set to true, the SOAP message of outgoing SOAP responses will be attached to the span.")
			.configurationCategory(SOAP_TRACING_PLUGIN)
			.buildWithDefault(false);

	@Override
	public List<Class<? extends StagemonitorPlugin>> dependsOn() {
		return Collections.<Class<? extends StagemonitorPlugin>>singletonList(TracingPlugin.class);
	}

	public boolean isSoapClientRecordRequestMessages() {
		return soapClientRecordRequestMessages.getValue();
	}

	public boolean isSoapClientRecordResponseMessages() {
		return soapClientRecordResponseMessages.getValue();
	}

	public boolean isSoapServerRecordRequestMessages() {
		return soapServerRecordRequestMessages.getValue();
	}

	public boolean isSoapServerRecordResponseMessages() {
		return soapServerRecordResponseMessages.getValue();
	}
}
