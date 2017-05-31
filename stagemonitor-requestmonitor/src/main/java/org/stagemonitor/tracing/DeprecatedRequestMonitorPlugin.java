package org.stagemonitor.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.StagemonitorPlugin;

@Deprecated
public class DeprecatedRequestMonitorPlugin extends StagemonitorPlugin {

	private static final Logger logger = LoggerFactory.getLogger(DeprecatedRequestMonitorPlugin.class);

	@Override
	public void initializePlugin(InitArguments initArguments) throws Exception {
		logger.warn("The stagemonitor-requestmonitor plugin is replaced by stagemonitor-tracing and is scheduled for deletion. " +
				"Please update your dependencies.");
	}
}
