package org.stagemonitor.web.servlet.eum;

import org.stagemonitor.tracing.B3HeaderFormat;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.util.IOUtils;
import org.stagemonitor.web.servlet.ServletPlugin;
import org.stagemonitor.web.servlet.eum.ClientSpanMetadataTagProcessor.ClientSpanMetadataDefinition;

import java.util.Map;

import static java.util.Collections.emptyMap;

public class WeaselClientSpanExtension extends ClientSpanExtension {

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
		return "ineum('traceId', '" + B3HeaderFormat.getTraceId(spanWrapper) + "');";
	}
}
