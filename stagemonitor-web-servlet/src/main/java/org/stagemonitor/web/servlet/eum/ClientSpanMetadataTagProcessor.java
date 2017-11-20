package org.stagemonitor.web.servlet.eum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.converter.AbstractValueConverter;
import org.stagemonitor.configuration.converter.DoubleValueConverter;
import org.stagemonitor.web.servlet.ServletPlugin;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.opentracing.Span;

public class ClientSpanMetadataTagProcessor extends ClientSpanTagProcessor {

	static final String TYPE_STRING = "string";
	static final String TYPE_NUMBER = "number";
	static final String TYPE_BOOLEAN = "boolean";
	public static final String REQUEST_PARAMETER_METADATA_PREFIX = "m_";
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final ServletPlugin servletPlugin;

	protected ClientSpanMetadataTagProcessor(ServletPlugin servletPlugin) {
		super(ClientSpanServlet.TYPE_PAGE_LOAD);
		this.servletPlugin = servletPlugin;
	}

	@Override
	protected void processSpanImpl(Span spanBuilder, Map<String, String[]> servletParameters) {
		final Map<String, ClientSpanMetadataDefinition> whitelistedClientSpanTags = servletPlugin.getWhitelistedClientSpanTags();
		for (String originalParameterName : servletParameters.keySet()) {
			final String parameterNameWithoutPrefix = stripMetadataPrefix(originalParameterName);
			if (originalParameterName.startsWith(REQUEST_PARAMETER_METADATA_PREFIX) && isParameterWhitelisted(whitelistedClientSpanTags, parameterNameWithoutPrefix)) {
				final String parameterValueOrNull = getParameterValueOrNull(originalParameterName, servletParameters);
				final ClientSpanMetadataDefinition clientSpanTagDefinition = whitelistedClientSpanTags.get(parameterNameWithoutPrefix);

				try {
					if (TYPE_STRING.equalsIgnoreCase(clientSpanTagDefinition.getType()) || parameterValueOrNull == null) {
						final String value = trimStringToLength(parameterValueOrNull, clientSpanTagDefinition.getLength());
						spanBuilder.setTag(parameterNameWithoutPrefix, value);
					} else if (TYPE_NUMBER.equalsIgnoreCase(clientSpanTagDefinition.getType())) {
						final Double value = DoubleValueConverter.INSTANCE.convert(parameterValueOrNull);
						spanBuilder.setTag(parameterNameWithoutPrefix, value);
					} else if (TYPE_BOOLEAN.equalsIgnoreCase(clientSpanTagDefinition.getType())) {
						final Boolean value = ClientSpanBooleanTagProcessor.parseBooleanOrFalse(parameterValueOrNull);
						spanBuilder.setTag(parameterNameWithoutPrefix, value);
					}
				} catch (IllegalArgumentException e) {
					// skip this metadata as it has either an invalid name or is not parsable
					logger.warn("error while parsing parameter name {} with value {}", parameterNameWithoutPrefix, parameterValueOrNull);
				}
			}
		}
	}

	private String stripMetadataPrefix(String parameterName) {
		if (parameterName.startsWith(REQUEST_PARAMETER_METADATA_PREFIX)) {
			return parameterName.substring(2);
		} else {
			return parameterName;
		}
	}

	private boolean isParameterWhitelisted(Map<String, ClientSpanMetadataDefinition> whitelistedClientSpanTags, String parameterName) {
		return whitelistedClientSpanTags.containsKey(parameterName);
	}

	/**
	 * This class represents a client span metadata definition (e.g. <code>string(100)</code> or <code>boolean</code>,
	 * see stagemonitor configuration option <code>stagemonitor.eum.whitelistedClientSpanTags</code>.
	 */
	public static class ClientSpanMetadataDefinition {
		private static final Pattern typeAndLengthPattern = Pattern.compile("\\s*(string|number|boolean)\\s*(\\(\\s*([0-9]+)\\s*\\))?\\s*");
		private final String type;
		private final Integer length;
		private final String definition;

		public ClientSpanMetadataDefinition(String definition) {
			this.definition = definition;
			final Matcher matcher = typeAndLengthPattern.matcher(definition);
			if (matcher.matches()) {
				this.type = matcher.group(1);
				String lengthOrNull = matcher.group(3);
				if (lengthOrNull == null) {
					length = MAX_LENGTH;
				} else {
					length = Integer.parseInt(lengthOrNull);
				}
			} else {
				throw new IllegalArgumentException("invalid client span metadata definition: " + definition);
			}
		}

		public String getType() {
			return type;
		}

		public Integer getLength() {
			return length;
		}

		public String getDefinition() {
			return definition;
		}
	}

	public static class ClientSpanMetadataConverter extends AbstractValueConverter<ClientSpanMetadataDefinition> {

		@Override
		public ClientSpanMetadataDefinition convert(String definition) throws IllegalArgumentException {
			return new ClientSpanMetadataDefinition(definition);
		}

		@Override
		public String toString(ClientSpanMetadataDefinition value) {
			return value.getDefinition();
		}

	}
}
