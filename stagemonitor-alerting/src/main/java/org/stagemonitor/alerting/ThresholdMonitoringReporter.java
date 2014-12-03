package org.stagemonitor.alerting;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import org.stagemonitor.core.util.JsonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class ThresholdMonitoringReporter extends ScheduledReporter {

	private List<Check> checks;
	private List<Alerter> alerters;
	private Map<String, Incident> currentIncidentsByCheckId = new HashMap<String, Incident>();

	protected ThresholdMonitoringReporter(MetricRegistry registry) {
		super(registry, "threshold-monitoring-reporter", MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
	}

	@Override
	public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
					   SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters,
					   SortedMap<String, Timer> timers) {

		for (Check check : checks) {
			checkMetrics(gauges, check);
			checkMetrics(counters, check);
			checkMetrics(histograms, check);
			checkMetrics(meters, check);
			checkMetrics(timers, check);
		}
	}

	private void checkMetrics(Map<String, ?> metrics, Check check) {
		for (Map.Entry<String, ?> entry : metrics.entrySet()) {
			if (check.getMetricName().matcher(entry.getKey()).matches()) {
				final double currentValue = JsonUtils.toObjectNode(entry.getValue()).get(check.getMetric()).asDouble();
				// only first incident is reported if regex matches multiple metrics
				addIncident(check, check.check(currentValue), currentValue);
				return;
			}
		}
	}

	private void addIncident(Check check, Check.Status status, double currentValue) {
		final Incident currentIncident = currentIncidentsByCheckId.get(check.getId());
		if (currentIncident == null && status != Check.Status.OK) {
			// only create incidents for non OK statuses
			createIncident(check, Check.Status.OK, status, currentValue);
		} else if (currentIncident != null && currentIncident.getNewStatus() != status) {
			// only update on state changes
			createIncident(check, currentIncident.getNewStatus(), status, currentValue);
		}
	}


	private void createIncident(Check check, Check.Status oldStatus, Check.Status newStatus, double currentValue) {
		final Incident incident = new Incident(oldStatus, newStatus, currentValue, check.getId(), "request.Search.time.p95 > 10");
		if (newStatus == Check.Status.OK) {
			currentIncidentsByCheckId.remove(check.getId());
		} else {
			currentIncidentsByCheckId.put(check.getId(), incident);
		}
		for (Alerter alerter : alerters) {
			alerter.alert(incident);
		}

	}
}
