package org.stagemonitor.tracing;

import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.tracing.utils.SpanUtils;

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

	public void monitorStart(MonitoredRequest monitoredRequest) {
		final long start = System.nanoTime();
		monitoredRequest.createSpan();
		final SpanContextInformation info = SpanContextInformation.getCurrent();
		if (info != null) {
			info.setOverhead1(System.nanoTime() - start);
		}
	}

	public void monitorStop() {
		final SpanContextInformation info = SpanContextInformation.getCurrent();
		if (info != null) {
			long overhead2 = System.nanoTime();
			info.getSpan().finish();
			trackOverhead(info.getOverhead1(), overhead2);
		}
	}

	public SpanContextInformation monitor(MonitoredRequest monitoredRequest) throws Exception {
		try {
			monitorStart(monitoredRequest);
			monitoredRequest.execute();
			return SpanContextInformation.getCurrent();
		} catch (Exception e) {
			recordException(e);
			throw e;
		} finally {
			monitorStop();
		}
	}

	public void recordException(Exception e) {
		SpanUtils.setException(TracingPlugin.getSpan(), e, tracingPlugin.getIgnoreExceptions(), tracingPlugin.getUnnestExceptions());
	}

	private void trackOverhead(long overhead1, long overhead2) {
		if (corePlugin.isInternalMonitoringActive()) {
			overhead2 = System.nanoTime() - overhead2;
			metricRegistry.timer(internalOverheadMetricName).update(overhead2 + overhead1, NANOSECONDS);
		}
	}

}
