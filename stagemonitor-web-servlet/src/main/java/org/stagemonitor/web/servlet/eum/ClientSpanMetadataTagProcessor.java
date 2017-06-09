package org.stagemonitor.web.servlet.eum;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.web.servlet.ServletPlugin;

import java.util.Map;

import io.opentracing.Tracer;

public class ClientSpanMetadataTagProcessor extends ClientSpanTagProcessor {

	private ServletPlugin webPlugin;

	protected ClientSpanMetadataTagProcessor() {
		super(ClientSpanServlet.TYPE_PAGE_LOAD);
		this.webPlugin = Stagemonitor.getPlugin(ServletPlugin.class);
	}

	@Override
	protected void processSpanBuilderImpl(Tracer.SpanBuilder spanBuilder, Map<String, String[]> servletParameters) {
		for (String parameterName : servletParameters.keySet()) {
			String metadataNameWithoutPrefix = stripMetadataPrefix(parameterName);
			if (isParameterWhitelisted(metadataNameWithoutPrefix)) {
				spanBuilder.withTag(metadataNameWithoutPrefix, getParameterValueOrNull(parameterName, servletParameters));
			}
		}
	}

	private String stripMetadataPrefix(String parameterName) {
		return parameterName.startsWith("m_") ? parameterName.substring(2) : "";
	}

	private boolean isParameterWhitelisted(String parameterName) {
		return webPlugin.getWhitelistedClientSpanTags().contains(parameterName);
	}

}
