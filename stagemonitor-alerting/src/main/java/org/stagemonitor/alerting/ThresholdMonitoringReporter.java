package org.stagemonitor.alerting;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.alerting.alerter.Alerter;
import org.stagemonitor.alerting.alerter.AlerterFactory;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.check.CheckGroup;
import org.stagemonitor.alerting.check.MetricCategory;
import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.alerting.incident.IncidentRepository;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.util.JsonUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class ThresholdMonitoringReporter extends ScheduledReporter {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final AlerterFactory alerterFactory;
	private final IncidentRepository incidentRepository;
	private final MeasurementSession measurementSession;
	private final AlertingPlugin alertingPlugin;

	protected ThresholdMonitoringReporter(MetricRegistry registry, AlertingPlugin alertingPlugin,
										  AlerterFactory alerterFactory, IncidentRepository incidentRepository,
										  MeasurementSession measurementSession) {
		super(registry, "threshold-monitoring-reporter", MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
		this.alertingPlugin = alertingPlugin;
		this.alerterFactory = alerterFactory;
		this.incidentRepository = incidentRepository;
		this.measurementSession = measurementSession;
	}

	@Override
	public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
					   SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters,
					   SortedMap<String, Timer> timers) {
		ObjectNode metrics = JsonUtils.getMapper().createObjectNode();
		metrics.set(MetricCategory.GAUGE.getPath(), JsonUtils.toObjectNode(gauges));
		metrics.set(MetricCategory.COUNTER.getPath(), JsonUtils.toObjectNode(counters));
		metrics.set(MetricCategory.HISTOGRAM.getPath(), JsonUtils.toObjectNode(histograms));
		metrics.set(MetricCategory.METER.getPath(), JsonUtils.toObjectNode(meters));
		metrics.set(MetricCategory.TIMER.getPath(), JsonUtils.toObjectNode(timers));

		for (CheckGroup check : alertingPlugin.getCheckGroups()) {
			if (measurementSession.getApplicationName().equals(check.getApplication()) && check.isActive()) {
				checkMetrics(metrics, check);
			}
		}
	}

	private void checkMetrics(JsonNode metrics, CheckGroup check) {
		List<Check.Result> checkResults = new LinkedList<Check.Result>();

		Iterator<Map.Entry<String, JsonNode>> metricsOfCategory = metrics.get(check.getMetricCategory().getPath()).fields();
		while (metricsOfCategory.hasNext()) {
			Map.Entry<String, JsonNode> metricTypes = metricsOfCategory.next();
			if (check.getTarget().matcher(metricTypes.getKey()).matches()) {
				Map<String, Double> valuesByMetricType = getValuesByMetricType(metricTypes.getValue());
				checkResults.addAll(check.checkAll(metricTypes.getKey(), valuesByMetricType));
			}
		}
		addIncident(check, checkResults);
	}

	private Map<String, Double> getValuesByMetricType(JsonNode metricTypes) {
		Map<String, Double> metricTypesMap = new HashMap<String, Double>();
		final Iterator<Map.Entry<String, JsonNode>> fields = metricTypes.fields();
		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> stringJsonNodeEntry = fields.next();
			metricTypesMap.put(stringJsonNodeEntry.getKey(), stringJsonNodeEntry.getValue().asDouble());
		}
		return metricTypesMap;
	}

	private void addIncident(CheckGroup checkGroup, List<Check.Result> results) {
		Incident incident = getAndPersistIncident(checkGroup, results);
		if (isAlertIncidents(checkGroup, incident)) {
			for (Alerter alerter : alerterFactory.getAlerters(incident)) {
				alerter.alert(incident);
			}
		}
	}

	private Incident getAndPersistIncident(CheckGroup checkGroup, List<Check.Result> results) {
		boolean sucessfullyPersisted = false;
		Incident incident = null;
		while (!sucessfullyPersisted) {
			incident = getOrCreateIncident(checkGroup, results);
			sucessfullyPersisted = trySaveOrDeleteIncident(checkGroup, incident);
		}
		return incident;
	}

	private boolean isAlertIncidents(CheckGroup checkGroup, Incident incident) {
		return (incident.hasStageChange() && incident.getConsecutiveFailures() >= checkGroup.getAlertAfterXFailures()) ||
				incident.getConsecutiveFailures() == checkGroup.getAlertAfterXFailures();
	}

	private Incident getOrCreateIncident(CheckGroup checkGroup, List<Check.Result> results) {
		final Incident currentIncident;
		Incident previousIncident = incidentRepository.getIncidentByCheckGroupId(checkGroup.getId());
		if (previousIncident == null) {
			currentIncident = new Incident(checkGroup, measurementSession, results);
		} else {
			currentIncident = new Incident(previousIncident, measurementSession, results);
		}

		return currentIncident;
	}

	private boolean trySaveOrDeleteIncident(CheckGroup checkGroup, Incident incident) {
		if (incident.getNewStatus() == Check.Status.OK) {
			if (!incidentRepository.deleteIncident(incident)) {
				logger.warn("Optimistic lock failure when deleting incident for check group {}.", checkGroup.getId());
				return false;
			}
		} else if (incident.getOldStatus() == null) {
			incident.setOldStatus(Check.Status.OK);
			if (!incidentRepository.createIncident(incident)) {
				logger.warn("Error while creating incident for check group {}. " +
						"A incident for the same check group already exists.", checkGroup.getId());
				return false;
			}
		} else if (!incidentRepository.updateIncident(incident)) {
			logger.warn("Optimistic lock failure when updating incident for check group {}.", checkGroup.getId());
			return false;
		}
		return true;
	}

}
