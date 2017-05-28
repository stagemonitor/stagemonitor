package org.stagemonitor.alerting.annotation;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.alerting.check.MetricValueType;
import org.stagemonitor.alerting.check.Threshold;
import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.tracing.MonitorRequests;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class SlaCheckCreatingClassPathScannerTest {

	private final AlertingPlugin alertingPlugin = Stagemonitor.getPlugin(AlertingPlugin.class);
	private Map<String, Check> checks;

	private static class SlaTestClass {
		@SLAs({
				@SLA(metric = {MetricValueType.P95, MetricValueType.MAX}, threshold = {0, 0}),
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
		@SLA(metric = {MetricValueType.P95, MetricValueType.MAX}, threshold = 0)
		void tooFewThresholds() {
		}

		@MonitorRequests(resolveNameAtRuntime = true)
		@SLA(metric = {MetricValueType.P95, MetricValueType.MAX}, threshold = {0, 0})
		void slaMonitorRequestsResolveAtRuntime() {
		}

		@MonitorRequests(requestName = "monitor requests custom name")
		@SLA(metric = {MetricValueType.P95, MetricValueType.MAX}, threshold = {0, 0})
		void slaMonitorRequestsCustomName() {
		}

		@Timed(name = "timed custom name", absolute = true)
		@SLA(metric = {MetricValueType.P95, MetricValueType.MAX}, threshold = {0, 0})
		void slaTimedCustomName() {
		}

		@Timed
		@SLA(metric = {MetricValueType.P95, MetricValueType.MAX}, threshold = {0, 0})
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
		Stagemonitor.startMonitoring();
	}

	@Before
	public void setUp() throws Exception {
		checks = alertingPlugin.getChecks();
	}

	@Test
	public void testSlaMonitorRequests() throws Exception {
		testErrorRateCheck("void org.stagemonitor.alerting.annotation.SlaCheckCreatingClassPathScannerTest$SlaTestClass.monitorSla().errors",
				name("error_rate").operationName("Monitor Sla").operationType("method_invocation").build());

		testResponseTimeCheck("void org.stagemonitor.alerting.annotation.SlaCheckCreatingClassPathScannerTest$SlaTestClass.monitorSla().responseTime",
				name("response_time").operationName("Monitor Sla").operationType("method_invocation").build());
	}

	private void testErrorRateCheck(String checkId, MetricName checkTarget) {
		final Check errorRateCheck = checks.get(checkId);
		assertNotNull(checks.keySet().toString(), errorRateCheck);
		assertEquals("Alerting-Test", errorRateCheck.getApplication());
		assertEquals(checkTarget, errorRateCheck.getTarget());
	}

	private void testResponseTimeCheck(String checkId, MetricName checkTarget) {
		final Check responseTimeChek = checks.get(checkId);
		assertNotNull(checks.keySet().toString(), responseTimeChek);
		assertEquals("Alerting-Test", responseTimeChek.getApplication());
		assertEquals(checkTarget, responseTimeChek.getTarget());
		final List<Threshold> thresholds = responseTimeChek.getThresholds(CheckResult.Status.ERROR);
		final Threshold p95 = thresholds.get(0);
		assertEquals(MetricValueType.P95, p95.getValueType());
		assertEquals(Threshold.Operator.LESS, p95.getOperator());
		assertEquals(0, p95.getThresholdValue(), 0);
		final Threshold max = thresholds.get(1);
		assertEquals(MetricValueType.MAX, max.getValueType());
		assertEquals(Threshold.Operator.LESS, max.getOperator());
		assertEquals(0, max.getThresholdValue(), 0);
	}

	@Test
	public void testSlaCustomName() throws Exception {
		testResponseTimeCheck("void org.stagemonitor.alerting.annotation.SlaCheckCreatingClassPathScannerTest$SlaTestClass.slaMonitorRequestsCustomName().responseTime",
				name("response_time").operationName("monitor requests custom name").operationType("method_invocation").build());
	}

	@Test
	public void testTimedCustomName() throws Exception {
		testResponseTimeCheck("void org.stagemonitor.alerting.annotation.SlaCheckCreatingClassPathScannerTest$SlaTestClass.slaTimedCustomName().responseTime",
				name("timer").tag("signature", "timed custom name").build());
	}

	@Test
	public void testSlaTimed() throws Exception {
		testResponseTimeCheck("void org.stagemonitor.alerting.annotation.SlaCheckCreatingClassPathScannerTest$SlaTestClass.slaOnTimed().responseTime",
				name("timer").tag("signature", "SlaCheckCreatingClassPathScannerTest$SlaTestClass#slaOnTimed").build());
	}

	@Test
	public void testSlaExceptionMetered() throws Exception {
		testErrorRateCheck("void org.stagemonitor.alerting.annotation.SlaCheckCreatingClassPathScannerTest$SlaTestClass.slaOnExceptionMetered().errors",
				name("exception_rate").tag("signature", "SlaCheckCreatingClassPathScannerTest$SlaTestClass#slaOnExceptionMetered").build());
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
				name("response_time").operationName("Sla Monitor Requests Class Level").operationType("method_invocation").build());
	}

	@MonitorRequests
	private static class ClassLevelMonitorRequestsTestClass {
		static void makeSureClassIsLoaded() {
		}
		@SLA(metric = {MetricValueType.P95, MetricValueType.MAX}, threshold = {0, 0})
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
