package org.stagemonitor.tracing;

import io.opentracing.Scope;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.tracing.utils.SpanUtils;

import io.opentracing.Span;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class RequestMonitor {

	private final MetricName internalOverheadMetricName = name("internal_overhead_request_monitor").build();

	private Metric2Registry metricRegistry;
	private CorePlugin corePlugin;
	private TracingPlugin tracingPlugin;

	public RequestMonitor(ConfigurationRegistry configuration, Metric2Registry registry) {
		this(configuration, registry, configuration.getConfig(TracingPlugin.class));
	}

	private RequestMonitor(ConfigurationRegistry configuration, Metric2Registry registry, TracingPlugin tracingPlugin) {
		this.metricRegistry = registry;
		this.corePlugin = configuration.getConfig(CorePlugin.class);
		this.tracingPlugin = tracingPlugin;
	}

	public SpanContextInformation monitorStart(MonitoredRequest monitoredRequest) {
		return monitorStart(monitoredRequest, true);
	}

	private SpanContextInformation monitorStart(MonitoredRequest monitoredRequest, boolean activateSpan) {
		if (! corePlugin.isStagemonitorActive()) {
			return null;
		}
		final long start = System.nanoTime();
		final Scope scope = monitoredRequest.createScope();
		final SpanContextInformation info = SpanContextInformation.get(scope.span());
		if (info != null) {
			info.setOverhead1(System.nanoTime() - start);
		}
		return info;
	}

	public void monitorStop() {
		if (! corePlugin.isStagemonitorActive()) {
			return;
		}
		final Scope activeScope = tracingPlugin.getTracer().scopeManager().active();
		if (activeScope != null) {
			final Span currentSpan = activeScope.span();
			final SpanContextInformation info = SpanContextInformation.get(currentSpan);
			if (info != null) {
				long overhead2 = System.nanoTime();
				trackOverhead(info.getOverhead1(), overhead2);
			}
			activeScope.close();
		}
	}

	public SpanContextInformation monitor(MonitoredRequest monitoredRequest) throws Exception {
		if (corePlugin.isStagemonitorActive()) {
			try {
				final SpanContextInformation info = monitorStart(monitoredRequest);
				monitoredRequest.execute();
				return info;
			} catch (Exception e) {
				recordException(e);
				throw e;
			} finally {
				monitorStop();
			}
		}
		return null;
	}

	public void recordException(Exception e) {
		if (! corePlugin.isStagemonitorActive()) {
			return;
		}
		final Scope activeScope = tracingPlugin.getTracer().scopeManager().active();
		if (activeScope != null) {
			SpanUtils.setException(activeScope.span(), e, tracingPlugin.getIgnoreExceptions(), tracingPlugin.getUnnestExceptions());
		}
	}

	private void trackOverhead(long overhead1, long overhead2) {
		if (corePlugin.isInternalMonitoringActive()) {
			overhead2 = System.nanoTime() - overhead2;
			metricRegistry.timer(internalOverheadMetricName).update(overhead2 + overhead1, NANOSECONDS);
		}
	}

}
