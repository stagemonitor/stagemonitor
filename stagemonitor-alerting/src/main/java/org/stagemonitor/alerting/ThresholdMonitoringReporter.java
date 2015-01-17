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
import org.stagemonitor.core.util.JsonUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class ThresholdMonitoringReporter extends ScheduledReporter {

	private final CheckGroupRepository checkGroupRepository;
	private final AlerterFactory alerterFactory;
	// TODO IncidentRepository - gotcha: different status on different host/instance
	private Map<String, Incident> currentIncidentsByCheckId = new HashMap<String, Incident>();

	protected ThresholdMonitoringReporter(MetricRegistry registry, CheckGroupRepository checkGroupRepository,
										  AlerterFactory alerterFactory) {
		super(registry, "threshold-monitoring-reporter", MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
		this.checkGroupRepository = checkGroupRepository;
		this.alerterFactory = alerterFactory;
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

		for (CheckGroup check : checkGroupRepository.getAllActiveCheckGroups()) {
			checkMetrics(metrics, check);
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
		addIncident(check, checkResults, Check.Result.getMostSevereStatus(checkResults));
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

	private void addIncident(CheckGroup checkGroup, List<Check.Result> results, Check.Status newStatus) {
		Incident incident = createIncident(checkGroup, results, newStatus);
		saveOrDeleteIncident(checkGroup, incident);
		if (isAlertIncidents(checkGroup, incident)) {
			for (Alerter alerter : alerterFactory.getAlerters(incident)) {
				alerter.alert(incident);
			}
		}
	}

	private boolean isAlertIncidents(CheckGroup checkGroup, Incident incident) {
		return (incident.hasStageChange() && incident.getConsecutiveFailures() >= checkGroup.getAlertAfterXFailures()) ||
				incident.getConsecutiveFailures() == checkGroup.getAlertAfterXFailures();
	}

	private Incident createIncident(CheckGroup checkGroup, List<Check.Result> results, Check.Status newStatus) {
		final Incident currentIncident;
		Incident lastIncident = currentIncidentsByCheckId.get(checkGroup.getId());
		if (lastIncident == null) {
			currentIncident = new Incident(checkGroup, Check.Status.OK, newStatus, results);
		} else {
			currentIncident = new Incident(checkGroup, lastIncident.getNewStatus(), newStatus, results);
			currentIncident.setConsecutiveFailures(lastIncident.getConsecutiveFailures());
		}
		if (newStatus != Check.Status.OK) {
			currentIncident.incrementConsecutiveFailures();
		} else {
			currentIncident.setConsecutiveFailures(0);
		}
		return currentIncident;
	}

	private void saveOrDeleteIncident(CheckGroup checkGroup, Incident incident) {
		if (incident.getNewStatus() == Check.Status.OK) {
			currentIncidentsByCheckId.remove(checkGroup.getId());
		} else {
			currentIncidentsByCheckId.put(checkGroup.getId(), incident);
		}
	}

}
