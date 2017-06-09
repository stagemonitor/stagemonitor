package org.stagemonitor.web.servlet.eum;

import org.stagemonitor.core.StagemonitorSPI;

import java.util.List;

public abstract class ClientSpanExtensionSPI implements StagemonitorSPI {

	public abstract String getClientTraceExtensionScript();

	public abstract List<String> getWhitelistedTags();

}
