package org.stagemonitor.web.servlet.eum;

import org.stagemonitor.tracing.B3HeaderFormat;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.util.IOUtils;
import org.stagemonitor.web.servlet.ServletPlugin;
import org.stagemonitor.web.servlet.eum.ClientSpanMetadataTagProcessor.ClientSpanMetadataDefinition;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.stagemonitor.web.servlet.eum.ClientSpanMetadataTagProcessor.REQUEST_PARAMETER_METADATA_PREFIX;

public class WeaselClientSpanExtension extends ClientSpanExtension {

	static final String METADATA_BACKEND_SPAN_ID = REQUEST_PARAMETER_METADATA_PREFIX + WeaselClientSpanExtension.BACKEND_SPAN_ID;
	private static final String BACKEND_SPAN_ID = "bs";
	private final ServletPlugin servletPlugin;

	public WeaselClientSpanExtension(ServletPlugin servletPlugin) {
		this.servletPlugin = servletPlugin;
	}

	@Override
	public String getClientTraceExtensionScriptStaticPart() {
		if (servletPlugin.getMinifyClientSpanScript()) {
			return IOUtils.getResourceAsString("eum.debug.js");
		} else {
			return IOUtils.getResourceAsString("eum.min.js");
		}
	}

	@Override
	public Map<String, ClientSpanMetadataDefinition> getWhitelistedTags() {
		return emptyMap();
	}

	@Override
	public String getClientTraceExtensionScriptDynamicPart(SpanWrapper spanWrapper) {
		final B3HeaderFormat.B3Identifiers b3Identifiers = B3HeaderFormat.getB3Identifiers(spanWrapper);
		return "ineum('traceId', '" + b3Identifiers.getTraceId() + "');\n"
				+ "  ineum('meta', '" + BACKEND_SPAN_ID + "', '" + b3Identifiers.getSpanId() + "');\n";
	}
}
