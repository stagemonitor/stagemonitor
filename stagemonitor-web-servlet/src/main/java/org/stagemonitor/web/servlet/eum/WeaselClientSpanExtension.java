package org.stagemonitor.web.servlet.eum;

import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.tracing.B3HeaderFormat;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.util.IOUtils;
import org.stagemonitor.web.servlet.ServletPlugin;
import org.stagemonitor.web.servlet.eum.ClientSpanMetadataTagProcessor.ClientSpanMetadataDefinition;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.stagemonitor.web.servlet.eum.ClientSpanMetadataTagProcessor.REQUEST_PARAMETER_METADATA_PREFIX;

public class WeaselClientSpanExtension extends ClientSpanExtension {

	private static final String BACKEND_SPAN_ID = "bs";
	static final String METADATA_BACKEND_SPAN_ID = REQUEST_PARAMETER_METADATA_PREFIX + BACKEND_SPAN_ID;
	private static final String BACKEND_SPAN_SAMPLING_FLAG = "bsp";
	static final String METADATA_BACKEND_SPAN_SAMPLING_FLAG = REQUEST_PARAMETER_METADATA_PREFIX + BACKEND_SPAN_SAMPLING_FLAG;
	private ServletPlugin servletPlugin;
	private TracingPlugin tracingPlugin;

	@Override
	public void init(ConfigurationRegistry config) {
		this.servletPlugin = config.getConfig(ServletPlugin.class);
		tracingPlugin = config.getConfig(TracingPlugin.class);
		servletPlugin.registerMinifyClientSpanScriptOptionChangedListener(new ConfigurationOption.ChangeListener<Boolean>() {
			@Override
			public void onChange(ConfigurationOption<?> configurationOption, Boolean oldValue, Boolean newValue) {
				if (servletPlugin.getClientSpanJavaScriptServlet() != null) {
					servletPlugin.getClientSpanJavaScriptServlet().rebuildJavaScriptAndEtag();
				}
			}
		});
		tracingPlugin.registerDefaultRateLimitSpansPercentChangeListener(new ConfigurationOption.ChangeListener<Double>() {
			@Override
			public void onChange(ConfigurationOption<?> configurationOption, Double oldValue, Double newValue) {
				if (servletPlugin.getClientSpanJavaScriptServlet() != null) {
					servletPlugin.getClientSpanJavaScriptServlet().rebuildJavaScriptAndEtag();
				}
			}
		});

	}

	@Override
	public String getClientTraceExtensionScriptStaticPart() {
		String eumJs;
		if (servletPlugin.getMinifyClientSpanScript()) {
			eumJs = IOUtils.getResourceAsString("eum.debug.js");
		} else {
			eumJs = IOUtils.getResourceAsString("eum.min.js");
		}
		eumJs += "\nineum('sampleRate', " + tracingPlugin.getDefaultRateLimitSpansPercent() + ")";
		return eumJs;
	}

	@Override
	public Map<String, ClientSpanMetadataDefinition> getWhitelistedTags() {
		return emptyMap();
	}

	@Override
	public String getClientTraceExtensionScriptDynamicPart(SpanWrapper spanWrapper) {
		final B3HeaderFormat.B3Identifiers b3Identifiers = B3HeaderFormat.getB3Identifiers(spanWrapper);
		return "ineum('traceId', '" + b3Identifiers.getTraceId() + "');\n" +
				"ineum('meta', '" + BACKEND_SPAN_SAMPLING_FLAG + "', '" + (tracingPlugin.isSampled(spanWrapper) ? 1 : 0) + "');\n" +
				"ineum('meta', '" + BACKEND_SPAN_ID + "', '" + b3Identifiers.getSpanId() + "');\n";
	}
}
