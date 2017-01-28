package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.core.StagemonitorSPI;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitor;

import io.opentracing.Span;

public abstract class SpanReporter implements StagemonitorSPI {

	public void init(InitArguments initArguments) {
	}

	/**
	 * Callback method that is called when a {@link Span} was created and is ready to be reported
	 *
	 * @param requestInformation The object which contains all information about the request
	 */
	public abstract void report(RequestMonitor.RequestInformation requestInformation) throws Exception;

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
	public abstract boolean isActive(RequestMonitor.RequestInformation requestInformation);

	/**
	 * @return <code>true</code>, if this {@link SpanReporter} needs access to the call tree
	 * ({@link org.stagemonitor.requestmonitor.profiler.CallStackElement})
	 *
	 * @see #isActive(RequestMonitor.RequestInformation)
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

}
