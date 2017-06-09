package org.stagemonitor.web.servlet.eum;

import java.util.List;

public interface ClientSpanExtensionSPI {

	String getClientTraceExtensionScript();

	List<String> getWhitelistedTags();

}
