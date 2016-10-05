package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.core.StagemonitorSPI;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestTrace;

import io.opentracing.Span;

public abstract class RequestTraceReporter implements StagemonitorSPI {

	public void init(InitArguments initArguments) {
	}

	/**
	 * Callback method that is called when a {@link RequestTrace} was created and is ready to be reported
	 *
	 * @param reportArguments The parameter object which contains the actual parameters
	 */
	public abstract void reportRequestTrace(ReportArguments reportArguments) throws Exception;

	/**
	 * Whether this {@link RequestTraceReporter} is active
	 * <p/>
	 * This method is called at most once from {@link RequestMonitor} for one request.
	 * That means that the result from the first evaluation is final.
	 * <p/>
	 * If none of the currently registered {@link RequestTraceReporter}s (
	 * see {@link RequestMonitor#requestTraceReporters}) which {@link #requiresCallTree()}
	 * is active (see {@link RequestMonitor#isAnyRequestTraceReporterActiveWhichNeedsTheCallTree(RequestTrace)},
	 * the profiler does not have to be enabled for the current request.
	 *
	 * @param isActiveArguments The parameter object which contains the actual parameters
	 * @return <code>true</code>, if this {@link RequestTraceReporter} is active, <code>false</code> otherwise
	 */
	public abstract boolean isActive(IsActiveArguments isActiveArguments);

	/**
	 * @return <code>true</code>, if this {@link RequestTraceReporter} needs access to the call tree
	 * ({@link RequestTrace#getCallStack()})
	 *
	 * @see #isActive(IsActiveArguments)
	 */
	public boolean requiresCallTree() {
		return true;
	}

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

		public Span getSpan() {
			return requestTrace.getSpan();
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

		public Span getSpan() {
			return requestTrace.getSpan();
		}
	}
}
