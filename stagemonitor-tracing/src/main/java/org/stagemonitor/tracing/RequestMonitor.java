package org.stagemonitor.tracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class RequestMonitor {

	private final MetricName internalOverheadMetricName = name("internal_overhead_request_monitor").build();

	private Metric2Registry metricRegistry;
	private CorePlugin corePlugin;
	private TracingPlugin tracingPlugin;

	private static final ThreadLocal<Map<Span, ScopeContextHelper>> currentScopeMapThreadLocal = new ThreadLocal<>();

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
		final Span span = monitoredRequest.createSpan();
		if (activateSpan) {
			ScopeContextHelper scopeContextHelper = new ScopeContextHelper();
			Span delegate = span;
			if (span instanceof SpanWrapper) {
				SpanWrapper spanWrapper = (SpanWrapper) span;
				scopeContextHelper.spanWrapper = spanWrapper;
				delegate = spanWrapper.getDelegate();
			}
			scopeContextHelper.scope = tracingPlugin.getTracer().activateSpan(delegate);

			Map<Span, ScopeContextHelper> scopeContextHelperMap = currentScopeMapThreadLocal.get();
			if (scopeContextHelperMap == null) {
				scopeContextHelperMap = new HashMap<>();
				currentScopeMapThreadLocal.set(scopeContextHelperMap);
			}
			scopeContextHelperMap.put(delegate, scopeContextHelper);
		}
		return getSpanContextInformation(start, span);
	}

	private SpanContextInformation getSpanContextInformation(long start, Span span) {
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
		final Span activeSpan = tracingPlugin.getTracer().activeSpan();
		Map<Span, ScopeContextHelper> scopeContextHelperMap = currentScopeMapThreadLocal.get();
		final ScopeContextHelper scopeContextHelper = scopeContextHelperMap.remove(activeSpan);
		if (scopeContextHelperMap.isEmpty()) {
			currentScopeMapThreadLocal.remove();
		}
		if (scopeContextHelper != null) {
			monitorStop(scopeContextHelper.scope, scopeContextHelper.spanWrapper != null ? scopeContextHelper.spanWrapper : activeSpan);
		} else {
			monitorStop(null, activeSpan);
		}
	}

	private void monitorStop(Scope scope, Span span) {
		if (! corePlugin.isStagemonitorActive()) {
			return;
		}

		if (span != null) {
			final SpanContextInformation info = SpanContextInformation.get(span);
			if (info != null) {
				long overhead2 = System.nanoTime();
				trackOverhead(info.getOverhead1(), overhead2);
			}
			span.finish();
			if (scope != null) {
				scope.close();
			}
		}
	}

	public SpanContextInformation monitor(MonitoredRequest monitoredRequest) throws Exception {
		if (corePlugin.isStagemonitorActive()) {
			final long start = System.nanoTime();
			final Span span = monitoredRequest.createSpan();
			final Scope scope = tracingPlugin.getTracer().activateSpan(span);

			try {
				final SpanContextInformation info = getSpanContextInformation(start, span);
				monitoredRequest.execute();
				return info;
			} catch (Exception e) {
				recordException(e);
				throw e;
			} finally {
				monitorStop(scope, span);
			}
		}
		return null;
	}

	public void recordException(Exception e) {
		if (! corePlugin.isStagemonitorActive()) {
			return;
		}
		final Span activeSpan = tracingPlugin.getTracer().activeSpan();
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

	private class ScopeContextHelper {
		SpanWrapper spanWrapper;
		Scope scope;
	}

}
