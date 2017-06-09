package org.stagemonitor.web.servlet.eum;

import org.stagemonitor.util.IOUtils;

import java.util.List;

import static java.util.Collections.emptyList;

public class WeaselClientSpanExtension implements ClientSpanExtensionSPI {

	@Override
	public String getClientTraceExtensionScript() {
		return IOUtils.getResourceAsString("eum.debug.js"); // TODO: minified and non-debug?
	}

	@Override
	public List<String> getWhitelistedTags() {
		return emptyList();
	}

}
