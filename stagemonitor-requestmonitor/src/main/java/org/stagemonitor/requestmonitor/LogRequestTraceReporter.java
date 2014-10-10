package org.stagemonitor.requestmonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.StageMonitor;

/**
 * An implementation of {@link RequestTraceReporter} that logs the {@link RequestTrace}
 */
public class LogRequestTraceReporter implements RequestTraceReporter {

	private static final Logger logger = LoggerFactory.getLogger(LogRequestTraceReporter.class);

	private final RequestMonitorPlugin requestMonitorPlugin;

	public LogRequestTraceReporter() {
		this(StageMonitor.getConfiguration(RequestMonitorPlugin.class));
	}

	public LogRequestTraceReporter(RequestMonitorPlugin requestMonitorPlugin) {
		this.requestMonitorPlugin = requestMonitorPlugin;
	}

	@Override
	public <T extends RequestTrace> void reportRequestTrace(T requestTrace) {
		if (logger.isInfoEnabled()) {
			long start = System.currentTimeMillis();
			StringBuilder log = new StringBuilder(10000);
			log.append("\n########## PerformanceStats ##########\n");
			log.append(requestTrace.toString());

			log.append("Printing stats took ").append(System.currentTimeMillis() - start).append(" ms\n");
			log.append("######################################\n\n\n");

			logger.info(log.toString());
		}
	}

	@Override
	public boolean isActive() {
		return requestMonitorPlugin.isLogCallStacks();
	}
}
