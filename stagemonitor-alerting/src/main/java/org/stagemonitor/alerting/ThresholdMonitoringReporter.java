package org.stagemonitor.alerting;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.alerting.alerter.AlertSender;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.alerting.incident.IncidentRepository;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.metrics.metrics2.ScheduledMetrics2Reporter;
import org.stagemonitor.core.util.JsonUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ThresholdMonitoringReporter extends ScheduledMetrics2Reporter {

	private static final int OPTIMISTIC_CONCURRENCY_CONTROL_RETRIES = 10;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final AlertSender alertSender;
	private final IncidentRepository incidentRepository;
	private final MeasurementSession measurementSession;
	private final AlertingPlugin alertingPlugin;

	public static ThresholdMonitoringReporterBuilder forRegistry(Metric2Registry registry) {
		return new ThresholdMonitoringReporterBuilder(registry);
	}

	public ThresholdMonitoringReporter(ThresholdMonitoringReporterBuilder builder) {
		super(builder);
		this.alertingPlugin = builder.getAlertingPlugin();
		this.alertSender = builder.getAlertSender();
		this.incidentRepository = builder.getIncidentRepository();
		this.measurementSession = builder.getMeasurementSession();
	}

	@Override
	public void reportMetrics(Map<MetricName, Gauge> gauges, Map<MetricName, Counter> counters, Map<MetricName, Histogram> histograms, Map<MetricName, Meter> meters, Map<MetricName, Timer> timers) {
		Map<String, Map<MetricName, Metric>> metricsGroupedByName = new HashMap<String, Map<MetricName, Metric>>();
		addMetrics(metricsGroupedByName, gauges);
		addMetrics(metricsGroupedByName, counters);
		addMetrics(metricsGroupedByName, histograms);
		addMetrics(metricsGroupedByName, meters);
		addMetrics(metricsGroupedByName, timers);

		for (Check check : alertingPlugin.getChecks().values()) {
			if (measurementSession.getApplicationName().equals(check.getApplication()) && check.isActive()) {
				checkMetrics(metricsGroupedByName, check);
			}
		}
	}

	private <T extends Metric> void addMetrics(Map<String, Map<MetricName, Metric>> metricsGroupedByName, Map<MetricName, T > metrics) {
		for (Map.Entry<MetricName, T> entry : metrics.entrySet()) {
			Map<MetricName, Metric> metricsForName = metricsGroupedByName.get(entry.getKey().getName());
			if (metricsForName == null) {
				metricsForName = new HashMap<MetricName, Metric>();
				metricsGroupedByName.put(entry.getKey().getName(), metricsForName);
			}
			metricsForName.put(entry.getKey(), entry.getValue());
		}
	}

	private void checkMetrics(Map<String, Map<MetricName, Metric>> metricsGroupedByName, Check check) {
		List<CheckResult> checkResults = new LinkedList<CheckResult>();

		Map<MetricName, Metric> metricsOfName = metricsGroupedByName.get(check.getTarget().getName());
		if (metricsOfName == null) {
			metricsOfName = Collections.emptyMap();
		}
		for (Map.Entry<MetricName, Metric> entry : metricsOfName.entrySet()) {
			if (entry.getKey().matches(check.getTarget())) {
				try {
					checkResults.addAll(check.check(entry.getKey(), asMap(entry.getValue())));
				} catch (RuntimeException e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}
		try {
			addIncident(check, checkResults);
		} catch (RuntimeException e) {
			logger.warn(e.getMessage(), e);
		}
	}

	private Map<String, Number> asMap(Metric metric) {
		return JsonUtils.getMapper().convertValue(metric, Map.class);
	}

	private void addIncident(Check check, List<CheckResult> results) {
		Incident incident = getAndPersistIncident(check, results);
		if (incident != null) {
			alertSender.sendAlerts(check, incident);
		}
	}

	private Incident getAndPersistIncident(Check check, List<CheckResult> results) {
		boolean sucessfullyPersisted = false;
		Incident incident = null;
		for (int i = 0; i < OPTIMISTIC_CONCURRENCY_CONTROL_RETRIES && !sucessfullyPersisted; i++) {
			incident = getOrCreateIncident(check, results);
			sucessfullyPersisted = trySaveOrDeleteIncident(check, incident);
		}
		if (!sucessfullyPersisted) {
			logger.error("Failed to save incident {} after {} retries.", incident, OPTIMISTIC_CONCURRENCY_CONTROL_RETRIES);
		}
		return incident;
	}

	private Incident getOrCreateIncident(Check check, List<CheckResult> results) {
		final Incident currentIncident;
		Incident previousIncident = incidentRepository.getIncidentByCheckId(check.getId());
		if (previousIncident == null) {
			if (CheckResult.getMostSevereStatus(results) == CheckResult.Status.OK) {
				return null;
			}
			currentIncident = new Incident(check, measurementSession, results);
		} else {
			currentIncident = new Incident(previousIncident, measurementSession, results);
		}

		return currentIncident;
	}

	private boolean trySaveOrDeleteIncident(Check check, Incident incident) {
		if (incident == null) {
			return true;
		}
		if (incident.getNewStatus() == CheckResult.Status.OK) {
			if (!incidentRepository.deleteIncident(incident)) {
				logger.warn("Optimistic lock failure when deleting incident for check group {}.", check.getId());
				return false;
			}
		} else if (incident.getOldStatus() == null) {
			incident.setOldStatus(CheckResult.Status.OK);
			if (!incidentRepository.createIncident(incident)) {
				logger.warn("Error while creating incident for check group {}. " +
						"A incident for the same check group already exists.", check.getId());
				return false;
			}
		} else if (!incidentRepository.updateIncident(incident)) {
			logger.warn("Optimistic lock failure when updating incident for check group {}.", check.getId());
			return false;
		}
		return true;
	}

	public static class ThresholdMonitoringReporterBuilder extends ScheduledMetrics2Reporter.Builder<ThresholdMonitoringReporter, ThresholdMonitoringReporterBuilder> {
		private AlertSender alertSender;
		private IncidentRepository incidentRepository;
		private MeasurementSession measurementSession;
		private AlertingPlugin alertingPlugin;

		private ThresholdMonitoringReporterBuilder(Metric2Registry registry) {
			super(registry, "threshold-monitoring-reporter");
		}

		@Override
		public ThresholdMonitoringReporter build() {
			return new ThresholdMonitoringReporter(this);
		}

		public AlertSender getAlertSender() {
			return alertSender;
		}

		public ThresholdMonitoringReporterBuilder alertSender(AlertSender alertSender) {
			this.alertSender = alertSender;
			return this;
		}

		public IncidentRepository getIncidentRepository() {
			return incidentRepository;
		}

		public ThresholdMonitoringReporterBuilder incidentRepository(IncidentRepository incidentRepository) {
			this.incidentRepository = incidentRepository;
			return this;
		}

		public MeasurementSession getMeasurementSession() {
			return measurementSession;
		}

		public ThresholdMonitoringReporterBuilder measurementSession(MeasurementSession measurementSession) {
			this.measurementSession = measurementSession;
			return this;
		}

		public AlertingPlugin getAlertingPlugin() {
			return alertingPlugin;
		}

		public ThresholdMonitoringReporterBuilder alertingPlugin(AlertingPlugin alertingPlugin) {
			this.alertingPlugin = alertingPlugin;
			return this;
		}
	}

}
