package org.stagemonitor.web.servlet.eum;

import org.stagemonitor.core.StagemonitorSPI;
import org.stagemonitor.web.servlet.eum.ClientSpanMetadataTagProcessor.ClientSpanMetadataDefinition;

import java.util.Map;

/**
 * Incubating, interface might change.
 */
public abstract class ClientSpanExtension implements StagemonitorSPI {

	/**
	 * This method returns the script, which shall be included in the end user monitoring script bundle.
	 *
	 * @return the end user monitoring script
	 */
	public abstract String getClientTraceExtensionScript();

	/**
	 * Returns a map of auto whitelisted tags.
	 * The key is the name of the tag used in the frontend, the value is a client tag definition
	 * (see configuration option stagemonitor.eum.whitelistedClientSpanTags)
	 *
	 * @return a map of auto whitelisted tags
	 */
	public abstract Map<String, ClientSpanMetadataDefinition> getWhitelistedTags();

}
