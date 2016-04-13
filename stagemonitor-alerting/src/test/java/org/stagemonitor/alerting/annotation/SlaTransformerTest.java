package org.stagemonitor.alerting.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.alerting.check.MetricCategory;
import org.stagemonitor.alerting.check.Threshold;
import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.requestmonitor.MonitorRequests;

public class SlaTransformerTest {

	private final AlertingPlugin alertingPlugin = Stagemonitor.getPlugin(AlertingPlugin.class);

	private static class SlaTestClass {
		@SLAs({
				@SLA(metric = {SLA.Metric.P95, SLA.Metric.MAX}, threshold = {0, 0}),
				@SLA(errorRateThreshold = 0)
		})
		@MonitorRequests
		void monitorSla() {
			throw null;
		}

		@SLA(errorRateThreshold = 0)
		void monitorRequestsAnnotationMissing() {
		}

		@MonitorRequests
		@SLA(metric = {SLA.Metric.P95, SLA.Metric.MAX}, threshold = 0)
		void tooFewThresholds() {
		}

		static void makeSureClassIsLoaded() {
		}
	}

	@BeforeClass
	@AfterClass
	public static void init() throws Exception {
		Stagemonitor.init();
		SlaTestClass.makeSureClassIsLoaded();
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testSlaCreatesChecks() throws Exception {
		final Map<String, Check> checks = alertingPlugin.getChecks();

		final Check errorRateCheck = checks
				.get("org.stagemonitor.alerting.annotation.SlaTransformerTest$SlaTestClass.monitorSla().errors");
		assertNotNull(checks.keySet().toString(), errorRateCheck);
		assertEquals("Alerting-Test", errorRateCheck.getApplication());
		assertEquals(MetricCategory.METER, errorRateCheck.getMetricCategory());
		assertEquals("\\Qerror_rate_server.Monitor-Sla.All\\E", errorRateCheck.getTarget().toString());

		final Check responseTimeChek = checks
				.get("org.stagemonitor.alerting.annotation.SlaTransformerTest$SlaTestClass.monitorSla().responseTime");
		assertNotNull(checks.keySet().toString(), responseTimeChek);
		assertEquals("Alerting-Test", responseTimeChek.getApplication());
		assertEquals(MetricCategory.TIMER, responseTimeChek.getMetricCategory());
		assertEquals("\\Qresponse_time_server.Monitor-Sla.All\\E", responseTimeChek.getTarget().toString());
		final List<Threshold> thresholds = responseTimeChek.getThresholds(CheckResult.Status.ERROR);
		final Threshold p95 = thresholds.get(0);
		assertEquals("p95", p95.getMetric());
		assertEquals(Threshold.Operator.GREATER_EQUAL, p95.getOperator());
		assertEquals(0, p95.getThresholdValue(), 0);
		final Threshold max = thresholds.get(1);
		assertEquals("max", max.getMetric());
		assertEquals(Threshold.Operator.GREATER_EQUAL, max.getOperator());
		assertEquals(0, max.getThresholdValue(), 0);
	}

	@Test
	public void testTriggersResponseTimeIncident() throws Exception {
		try {
			new SlaTestClass().monitorSla();
		} catch (Exception e) {
			// ignore
		}
		alertingPlugin.getThresholdMonitoringReporter().report();
		final Incident incident = alertingPlugin.getIncidentRepository()
				.getIncidentByCheckId("org.stagemonitor.alerting.annotation.SlaTransformerTest$SlaTestClass.monitorSla().responseTime");
		assertNotNull(incident);
	}

	@Test
	public void testTriggersErrorRateIncident() throws Exception {
		try {
			new SlaTestClass().monitorSla();
		} catch (Exception e) {
			// ignore
		}

		alertingPlugin.getThresholdMonitoringReporter().report();
		final Incident incident = alertingPlugin.getIncidentRepository()
				.getIncidentByCheckId("org.stagemonitor.alerting.annotation.SlaTransformerTest$SlaTestClass.monitorSla().errors");
		assertNotNull(incident);
	}
}