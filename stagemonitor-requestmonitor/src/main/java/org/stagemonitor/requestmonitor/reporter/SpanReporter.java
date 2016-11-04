package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.core.StagemonitorSPI;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;

import java.util.HashMap;
import java.util.Map;

import io.opentracing.Span;

public abstract class SpanReporter implements StagemonitorSPI {

	public void init(InitArguments initArguments) {
	}

	/**
	 * Callback method that is called when a {@link Span} was created and is ready to be reported
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
	 * is active (see {@link RequestMonitor#isAnyRequestTraceReporterActiveWhichNeedsTheCallTree(RequestMonitor.RequestInformation)},
	 * the profiler does not have to be enabled for the current request.
	 *
	 * @param isActiveArguments The parameter object which contains the actual parameters
	 * @return <code>true</code>, if this {@link SpanReporter} is active, <code>false</code> otherwise
	 */
	public abstract boolean isActive(IsActiveArguments isActiveArguments);

	/**
	 * @return <code>true</code>, if this {@link SpanReporter} needs access to the call tree
	 * ({@link org.stagemonitor.requestmonitor.profiler.CallStackElement})
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
		private final Span span;
		private final Map<String, Object> requestAttributes;

		/**
		 * Only to be used in unit tests
		 */
		@Deprecated
		public IsActiveArguments(Span span) {
			this(span, new HashMap<String, Object>());
		}

		public IsActiveArguments(Span span, Map<String, Object> requestAttributes) {
			this.span = span;
			this.requestAttributes = requestAttributes;
		}

		public Span getSpan() {
			return span;
		}

		public Map<String, Object> getRequestAttributes() {
			return requestAttributes;
		}
	}

	public static class ReportArguments {
		private final Span span;
		private final CallStackElement callTree;
		private final Map<String, Object> requestAttributes;

		/**
		 * Only to be used in unit tests
		 */
		@Deprecated
		public ReportArguments(Span span, CallStackElement callTree) {
			this(span, callTree, new HashMap<String, Object>());
		}

		/**
		 * @param span
		 * @param callTree
		 */
		public ReportArguments(Span span, CallStackElement callTree, Map<String, Object> requestAttributes) {
			this.span = span;
			this.callTree = callTree;
			this.requestAttributes = requestAttributes;
		}

		public Span getSpan() {
			return span;
		}

		public com.uber.jaeger.Span getInternalSpan() {
			return (com.uber.jaeger.Span) span;
		}

		public CallStackElement getCallTree() {
			return callTree;
		}

		public Map<String, Object> getRequestAttributes() {
			return requestAttributes;
		}
	}
}
