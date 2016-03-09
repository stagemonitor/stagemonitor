package org.stagemonitor.requestmonitor.reporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;

/**
 * An implementation of {@link RequestTraceReporter} that logs the {@link RequestTrace}
 */
public class LogRequestTraceReporter extends RequestTraceReporter {

	private static final Logger logger = LoggerFactory.getLogger(LogRequestTraceReporter.class);

	private RequestMonitorPlugin requestMonitorPlugin;

	@Override
	public void init(InitArguments initArguments) {
		this.requestMonitorPlugin = initArguments.getConfiguration().getConfig(RequestMonitorPlugin.class);
	}

	@Override
	public void reportRequestTrace(ReportArguments reportArguments) {
		if (logger.isInfoEnabled()) {
			long start = System.currentTimeMillis();
			StringBuilder log = new StringBuilder(10000);
			log.append("\n########## PerformanceStats ##########\n");
			log.append(reportArguments.getRequestTrace().toString());

			log.append("Printing stats took ").append(System.currentTimeMillis() - start).append(" ms\n");
			log.append("######################################\n\n\n");

			logger.info(log.toString());
		}
	}

	@Override
	public boolean isActive(IsActiveArguments isActiveArguments) {
		return requestMonitorPlugin.isLogCallStacks();
	}
}
