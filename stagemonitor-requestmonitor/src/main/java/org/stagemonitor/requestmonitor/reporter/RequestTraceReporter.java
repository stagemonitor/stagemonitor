package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.core.StagemonitorSPI;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.requestmonitor.RequestTrace;

public abstract class RequestTraceReporter implements StagemonitorSPI {

	public void init(InitArguments initArguments) {
	}

	/**
	 * Callback method that is called when a {@link RequestTrace} was created and is ready to be reported
	 *
	 * @param reportArguments
	 */
	public abstract void reportRequestTrace(ReportArguments reportArguments) throws Exception;

	/**
	 * Whether this {@link RequestTraceReporter} is active
	 * <p/>
	 * This method is called at most once from {@link org.stagemonitor.requestmonitor.RequestMonitor} for one request.
	 * That means that the result from the first evaluation is final.
	 *
	 * @return <code>true</code>, if this {@link RequestTraceReporter} is active, <code>false</code> otherwise
	 * @param isActiveArguments
	 */
	public abstract boolean isActive(IsActiveArguments isActiveArguments);

	public void close(CloseArguments initArguments) {
	}

	public static class InitArguments {
		private final Configuration configuration;

		public InitArguments(Configuration configuration) {
			this.configuration = configuration;
		}

		public Configuration getConfiguration() {
			return configuration;
		}
	}

	public static class CloseArguments {
	}

	public static class IsActiveArguments {
		private final RequestTrace requestTrace;

		public IsActiveArguments(RequestTrace requestTrace) {
			this.requestTrace = requestTrace;
		}

		public RequestTrace getRequestTrace() {
			return requestTrace;
		}
	}

	public static class ReportArguments {
		private final RequestTrace requestTrace;

		/**
		 * @param requestTrace the {@link RequestTrace} of the current request
		 */
		public ReportArguments(RequestTrace requestTrace) {
			this.requestTrace = requestTrace;
		}

		public RequestTrace getRequestTrace() {
			return requestTrace;
		}
	}
}
