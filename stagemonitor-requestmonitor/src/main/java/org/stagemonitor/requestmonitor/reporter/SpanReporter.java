package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.core.StagemonitorSPI;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestTrace;

import io.opentracing.Span;

public abstract class SpanReporter implements StagemonitorSPI {

	public void init(InitArguments initArguments) {
	}

	/**
	 * Callback method that is called when a {@link RequestTrace} was created and is ready to be reported
	 *
	 * @param reportArguments The parameter object which contains the actual parameters
	 */
	public abstract void report(ReportArguments reportArguments) throws Exception;

	/**
	 * Whether this {@link SpanReporter} is active
	 * <p/>
	 * This method is called at most once from {@link RequestMonitor} for one request.
	 * That means that the result from the first evaluation is final.
	 * <p/>
	 * If none of the currently registered {@link SpanReporter}s (
	 * see {@link RequestMonitor#spanReporters}) which {@link #requiresCallTree()}
	 * is active (see {@link RequestMonitor#isAnyRequestTraceReporterActiveWhichNeedsTheCallTree(RequestTrace)},
	 * the profiler does not have to be enabled for the current request.
	 *
	 * @param isActiveArguments The parameter object which contains the actual parameters
	 * @return <code>true</code>, if this {@link SpanReporter} is active, <code>false</code> otherwise
	 */
	public abstract boolean isActive(IsActiveArguments isActiveArguments);

	/**
	 * @return <code>true</code>, if this {@link SpanReporter} needs access to the call tree
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
		private final Metric2Registry metricRegistry;

		public InitArguments(Configuration configuration, Metric2Registry metricRegistry) {
			this.configuration = configuration;
			this.metricRegistry = metricRegistry;
		}

		public Configuration getConfiguration() {
			return configuration;
		}

		public Metric2Registry getMetricRegistry() {
			return metricRegistry;
		}
	}

	public static class CloseArguments {
	}

	public static class IsActiveArguments {
		private final RequestTrace requestTrace;
		private final Span span;

		public IsActiveArguments(RequestTrace requestTrace, Span span) {
			this.requestTrace = requestTrace;
			this.span = span;
		}

		public RequestTrace getRequestTrace() {
			return requestTrace;
		}

		public Span getSpan() {
			return span;
		}
	}

	public static class ReportArguments {
		private final RequestTrace requestTrace;
		private final Span span;

		/**
		 * @param requestTrace the {@link RequestTrace} of the current request
		 * @param span
		 */
		public ReportArguments(RequestTrace requestTrace, Span span) {
			this.requestTrace = requestTrace;
			this.span = span;
		}

		public RequestTrace getRequestTrace() {
			return requestTrace;
		}

		public Span getSpan() {
			return span;
		}

		public com.uber.jaeger.Span getInternalSpan() {
			return (com.uber.jaeger.Span) span;
		}

	}
}
