package org.stagemonitor.alerting.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
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

public class SlaCheckCreatingClassPathScannerTest {

	private final AlertingPlugin alertingPlugin = Stagemonitor.getPlugin(AlertingPlugin.class);
	private Map<String, Check> checks;

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

		@MonitorRequests(resolveNameAtRuntime = true)
		@SLA(metric = {SLA.Metric.P95, SLA.Metric.MAX}, threshold = {0, 0})
		void slaMonitorRequestsResolveAtRuntime() {
		}

		@MonitorRequests(requestName = "monitor requests custom name")
		@SLA(metric = {SLA.Metric.P95, SLA.Metric.MAX}, threshold = {0, 0})
		void slaMonitorRequestsCustomName() {
		}

		@Timed(name = "timed custom name", absolute = true)
		@SLA(metric = {SLA.Metric.P95, SLA.Metric.MAX}, threshold = {0, 0})
		void slaTimedCustomName() {
		}

		@Timed
		@SLA(metric = {SLA.Metric.P95, SLA.Metric.MAX}, threshold = {0, 0})
		void slaOnTimed() {
		}

		@ExceptionMetered
		@SLA(errorRateThreshold = 0)
		void slaOnExceptionMetered() {
		}

		@Timed
		@SLA(errorRateThreshold = 0)
		void slaMissingExceptionMetered() {
		}

		static void makeSureClassIsLoaded() {
		}
	}

	@BeforeClass
	@AfterClass
	public static void init() throws Exception {
		Stagemonitor.init();
		SlaTestClass.makeSureClassIsLoaded();
		ClassLevelMonitorRequestsTestClass.makeSureClassIsLoaded();
		Stagemonitor.startMonitoring().get();
	}

	@Before
	public void setUp() throws Exception {
		checks = alertingPlugin.getChecks();
	}

	@Test
	public void testSlaMonitorRequests() throws Exception {
		testErrorRateCheck("void org.stagemonitor.alerting.annotation.SlaCheckCreatingClassPathScannerTest$SlaTestClass.monitorSla().errors",
				"\\Qerror_rate_server.Monitor-Sla.All\\E");

		testResponseTimeCheck("void org.stagemonitor.alerting.annotation.SlaCheckCreatingClassPathScannerTest$SlaTestClass.monitorSla().responseTime",
				"\\Qresponse_time_server.Monitor-Sla.All\\E");
	}

	private void testErrorRateCheck(String checkId, String checkTargetRegex) {
		final Check errorRateCheck = checks.get(checkId);
		assertNotNull(checks.keySet().toString(), errorRateCheck);
		assertEquals("Alerting-Test", errorRateCheck.getApplication());
		assertEquals(MetricCategory.METER, errorRateCheck.getMetricCategory());
		assertEquals(checkTargetRegex, errorRateCheck.getTarget().toString());
	}

	private void testResponseTimeCheck(String checkId, String checkTargetRegex) {
		final Check responseTimeChek = checks.get(checkId);
		assertNotNull(checks.keySet().toString(), responseTimeChek);
		assertEquals("Alerting-Test", responseTimeChek.getApplication());
		assertEquals(MetricCategory.TIMER, responseTimeChek.getMetricCategory());
		assertEquals(checkTargetRegex, responseTimeChek.getTarget().toString());
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
	public void testSlaCustomName() throws Exception {
		testResponseTimeCheck("void org.stagemonitor.alerting.annotation.SlaCheckCreatingClassPathScannerTest$SlaTestClass.slaMonitorRequestsCustomName().responseTime",
				"\\Qresponse_time_server.monitor-requests-custom-name.All\\E");
	}

	@Test
	public void testTimedCustomName() throws Exception {
		testResponseTimeCheck("void org.stagemonitor.alerting.annotation.SlaCheckCreatingClassPathScannerTest$SlaTestClass.slaTimedCustomName().responseTime",
				"\\Qtimer.timed-custom-name\\E");
	}

	@Test
	public void testSlaTimed() throws Exception {
		testResponseTimeCheck("void org.stagemonitor.alerting.annotation.SlaCheckCreatingClassPathScannerTest$SlaTestClass.slaOnTimed().responseTime",
				"\\Qtimer.SlaCheckCreatingClassPathScannerTest$SlaTestClass#slaOnTimed\\E");
	}

	@Test
	public void testSlaExceptionMetered() throws Exception {
		testErrorRateCheck("void org.stagemonitor.alerting.annotation.SlaCheckCreatingClassPathScannerTest$SlaTestClass.slaOnExceptionMetered().errors",
				"\\Qexception_rate.SlaCheckCreatingClassPathScannerTest$SlaTestClass#slaOnExceptionMetered\\E");
	}

	@Test
	public void testSlaMissingExceptionMetered() throws Exception {
		assertNull(checks.get("void org.stagemonitor.alerting.annotation.SlaCheckCreatingClassPathScannerTest$SlaTestClass.slaMissingExceptionMetered().errors"));
	}

	@Test
	public void testSlaMonitorRequestsResolveAtRuntime() throws Exception {
		assertNull(checks.get("void org.stagemonitor.alerting.annotation.SlaCheckCreatingClassPathScannerTest$SlaTestClass.slaMonitorRequestsResolveAtRuntime().responseTime"));
	}

	@Test
	public void testSlaMonitorRequestsClassLevel() throws Exception {
		testResponseTimeCheck("public void org.stagemonitor.alerting.annotation.SlaCheckCreatingClassPathScannerTest$ClassLevelMonitorRequestsTestClass.slaMonitorRequestsClassLevel().responseTime",
				"\\Qresponse_time_server.Sla-Monitor-Requests-Class-Level.All\\E");
	}

	@MonitorRequests
	private static class ClassLevelMonitorRequestsTestClass {
		static void makeSureClassIsLoaded() {
		}
		@SLA(metric = {SLA.Metric.P95, SLA.Metric.MAX}, threshold = {0, 0})
		public void slaMonitorRequestsClassLevel() {
		}
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
				.getIncidentByCheckId("void org.stagemonitor.alerting.annotation.SlaCheckCreatingClassPathScannerTest$SlaTestClass.monitorSla().responseTime");
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
				.getIncidentByCheckId("void org.stagemonitor.alerting.annotation.SlaCheckCreatingClassPathScannerTest$SlaTestClass.monitorSla().errors");
		assertNotNull(incident);
	}
}