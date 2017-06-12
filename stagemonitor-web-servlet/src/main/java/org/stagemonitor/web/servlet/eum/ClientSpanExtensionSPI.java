package org.stagemonitor.web.servlet.eum;

import org.stagemonitor.core.StagemonitorSPI;

import java.util.Map;

public abstract class ClientSpanExtensionSPI implements StagemonitorSPI {

	public abstract String getClientTraceExtensionScript();

	public abstract Map<String, String> getWhitelistedTags();

}
