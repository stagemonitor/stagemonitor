package org.stagemonitor.tracing;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.tracing.utils.SpanUtils;

import io.opentracing.Span;

import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class RequestMonitor {

	private final MetricName internalOverheadMetricName = name("internal_overhead_request_monitor").build();

	private Metric2Registry metricRegistry;
	private CorePlugin corePlugin;
	private TracingPlugin tracingPlugin;

	private final ThreadLocal<Scope> currentScope = new ThreadLocal<Scope>();

	public RequestMonitor(ConfigurationRegistry configuration, Metric2Registry registry) {
		this(configuration, registry, configuration.getConfig(TracingPlugin.class));
	}

	private RequestMonitor(ConfigurationRegistry configuration, Metric2Registry registry, TracingPlugin tracingPlugin) {
		this.metricRegistry = registry;
		this.corePlugin = configuration.getConfig(CorePlugin.class);
		this.tracingPlugin = tracingPlugin;
	}

	public SpanContextInformation monitorStart(MonitoredRequest monitoredRequest) {
		if (! corePlugin.isStagemonitorActive()) {
			return null;
		}
		final long start = System.nanoTime();
		final Span span = monitorStart(monitoredRequest, true);
		return getSpanContextInformation(span, start);
	}

	private Span monitorStart(MonitoredRequest monitoredRequest, boolean activateSpan) {
		final Span span = monitoredRequest.createSpan();
		if (activateSpan) {
			Scope scope = tracingPlugin.getTracer().activateSpan(span);
			currentScope.set(scope);
		}

		return span;
	}

	private SpanContextInformation getSpanContextInformation(Span span, long start) {
		final SpanContextInformation info = SpanContextInformation.get(span);
		if (info != null) {
			info.setOverhead1(System.nanoTime() - start);
		}
		return info;
	}

	public void monitorStop() {
		if (! corePlugin.isStagemonitorActive()) {
			return;
		}
		final Span activeSpan = tracingPlugin.getTracer().scopeManager().activeSpan();
		monitorStop(activeSpan);
	}

	private void monitorStop(Span span) {
		if (span != null) {
			final SpanContextInformation info = SpanContextInformation.get(span);
			if (info != null) {
				long overhead2 = System.nanoTime();
				trackOverhead(info.getOverhead1(), overhead2);
			}
			span.finish();
			Scope scope = currentScope.get();
			if (scope != null) {
				scope.close();
			}
		}
	}

	public SpanContextInformation monitor(MonitoredRequest monitoredRequest) throws Exception {
		if (corePlugin.isStagemonitorActive()) {
			final long start = System.nanoTime();
			final Span span = monitorStart(monitoredRequest, false);
			final SpanContextInformation info = getSpanContextInformation(span, start);
			final Scope scope = tracingPlugin.getTracer().activateSpan(span);
			try {
				monitoredRequest.execute();
				return info;
			} catch (Exception e) {
				recordException(e);
				throw e;
			} finally {
				monitorStop(span);
				if (scope != null) {
					scope.close();
				}
			}
		}
		return null;
	}

	public void recordException(Exception e) {
		if (! corePlugin.isStagemonitorActive()) {
			return;
		}
		final Span activeSpan = tracingPlugin.getTracer().scopeManager().activeSpan();
		if (activeSpan != null) {
			SpanUtils.setException(activeSpan, e, tracingPlugin.getIgnoreExceptions(), tracingPlugin.getUnnestExceptions());
		}
	}

	private void trackOverhead(long overhead1, long overhead2) {
		if (corePlugin.isInternalMonitoringActive()) {
			overhead2 = System.nanoTime() - overhead2;
			metricRegistry.timer(internalOverheadMetricName).update(overhead2 + overhead1, NANOSECONDS);
		}
	}

}
