package org.stagemonitor.web.servlet.eum;

import org.stagemonitor.util.IOUtils;

import java.util.Map;

import static java.util.Collections.emptyMap;

public class WeaselClientSpanExtension extends ClientSpanExtensionSPI {

	@Override
	public String getClientTraceExtensionScript() {
		return IOUtils.getResourceAsString("eum.debug.js"); // TODO: minified and non-debug?
	}

	@Override
	public Map<String, String> getWhitelistedTags() {
		return emptyMap();
	}

}
