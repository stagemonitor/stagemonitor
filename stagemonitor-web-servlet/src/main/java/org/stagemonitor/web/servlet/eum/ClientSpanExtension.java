package org.stagemonitor.web.servlet.eum;

import org.stagemonitor.core.StagemonitorSPI;

import java.util.Map;

/**
 * Incubating, interface might change.
 */
public abstract class ClientSpanExtension implements StagemonitorSPI {

	public abstract String getClientTraceExtensionScript();

	public abstract Map<String, String> getWhitelistedTags();

}
