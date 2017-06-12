package org.stagemonitor.web.servlet.eum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.converter.DoubleValueConverter;
import org.stagemonitor.web.servlet.ServletPlugin;

import java.util.Map;

import io.opentracing.Tracer;

public class ClientSpanMetadataTagProcessor extends ClientSpanTagProcessor {

	public static final String TYPE_STRING = "string";
	public static final String TYPE_NUMBER = "number";
	public static final String TYPE_BOOLEAN = "boolean";
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private ServletPlugin servletPlugin;

	protected ClientSpanMetadataTagProcessor(ServletPlugin servletPlugin) {
		super(ClientSpanServlet.TYPE_PAGE_LOAD);
		this.servletPlugin = servletPlugin;
	}

	@Override
	protected void processSpanBuilderImpl(Tracer.SpanBuilder spanBuilder, Map<String, String[]> servletParameters) {
		for (String parameterName : servletParameters.keySet()) {
			String metadataNameWithoutPrefix = stripMetadataPrefix(parameterName);
			if (isParameterWhitelisted(metadataNameWithoutPrefix)) {

				final Map<String, String> whitelistedClientSpanTags = servletPlugin.getWhitelistedClientSpanTags();
				final String parameterValueOrNull = getParameterValueOrNull(parameterName, servletParameters);
				final String type = whitelistedClientSpanTags.get(metadataNameWithoutPrefix);

				if (TYPE_STRING.equalsIgnoreCase(type) || parameterValueOrNull == null) {
					spanBuilder.withTag(metadataNameWithoutPrefix, parameterValueOrNull);
				} else if (TYPE_NUMBER.equalsIgnoreCase(type)) {
					final Double value = DoubleValueConverter.INSTANCE.convert(parameterValueOrNull);
					spanBuilder.withTag(metadataNameWithoutPrefix, value);
				} else if (TYPE_BOOLEAN.equalsIgnoreCase(type)) {
					final Boolean value = ClientSpanBooleanTagProcessor.parseBooleanOrFalse(parameterValueOrNull);
					spanBuilder.withTag(metadataNameWithoutPrefix, value);
				} else {
					logger.error(
							"{} is not a valid type for client span metadata values. Valid ones are: {}, {} and {}",
							type, TYPE_STRING, TYPE_BOOLEAN, TYPE_NUMBER);
				}

			}
		}
	}

	private String stripMetadataPrefix(String parameterName) {
		return parameterName.startsWith("m_") ? parameterName.substring(2) : "";
	}

	private boolean isParameterWhitelisted(String parameterName) {
		return servletPlugin.getWhitelistedClientSpanTags().containsKey(parameterName);
	}

}
