package org.stagemonitor.alerting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.timer;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Timer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.stagemonitor.alerting.alerter.AlertSender;
import org.stagemonitor.alerting.alerter.Alerter;
import org.stagemonitor.alerting.alerter.Subscription;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.alerting.check.MetricCategory;
import org.stagemonitor.alerting.check.Threshold;
import org.stagemonitor.alerting.incident.CheckResults;
import org.stagemonitor.alerting.incident.ConcurrentMapIncidentRepository;
import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.alerting.incident.IncidentRepository;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.JsonUtils;

public class ThresholdMonitoringReporterTest {

	private final MeasurementSession measurementSession = new MeasurementSession("testApp", "testHost", "testInstance");
	private ThresholdMonitoringReporter thresholdMonitoringReporter;
	private Alerter alerter;
	private IncidentRepository incidentRepository;
	private AlertingPlugin alertingPlugin;

	@Before
	public void setUp() throws Exception {
		Configuration configuration = mock(Configuration.class);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(mock(CorePlugin.class));
		alertingPlugin = mock(AlertingPlugin.class);
		Subscription subscription = new Subscription();
		subscription.setAlerterType("Test Alerter");
		subscription.setAlertOnBackToOk(true);
		subscription.setAlertOnWarn(true);
		subscription.setAlertOnError(true);
		subscription.setAlertOnCritical(true);
		when(alertingPlugin.getSubscriptionsByIds()).thenReturn(Collections.singletonMap("1", subscription));
		when(configuration.getConfig(AlertingPlugin.class)).thenReturn(alertingPlugin);
		alerter = mock(Alerter.class);
		when(alerter.getAlerterType()).thenReturn("Test Alerter");
		when(alerter.isAvailable()).thenReturn(true);
		AlertSender alertSender = new AlertSender(configuration, Collections.singletonList(alerter));

		incidentRepository = spy(new ConcurrentMapIncidentRepository(new ConcurrentHashMap<String, Incident>()));
		thresholdMonitoringReporter = ThresholdMonitoringReporter.forRegistry(new Metric2Registry())
				.alertingPlugin(alertingPlugin)
				.alertSender(alertSender)
				.incidentRepository(incidentRepository)
				.measurementSession(measurementSession)
				.build();
	}

	@Test
	public void testAlerting() throws Exception {
		Check check = createCheckCheckingMean(1, 5);

		when(alertingPlugin.getChecks()).thenReturn(Collections.singletonMap(check.getId(), check));

		checkMetrics();

		ArgumentCaptor<Alerter.AlertArguments> alertArguments = ArgumentCaptor.forClass(Alerter.AlertArguments.class);
		verify(alerter).alert(alertArguments.capture());
		final Incident incident = alertArguments.getValue().getIncident();
		assertEquals(check.getId(), incident.getCheckId());
		assertEquals("Test Timer", incident.getCheckName());
		assertEquals(CheckResult.Status.OK, incident.getOldStatus());
		assertEquals(CheckResult.Status.WARN, incident.getNewStatus());

		assertEquals(1, incident.getCheckResults().size());
		List<CheckResult> checkResults = incident.getCheckResults().iterator().next().getResults();
		assertEquals(2, checkResults.size());

		CheckResult result = checkResults.get(0);
		assertEquals("test_timer,signature=timer3 mean >= 5.0", result.getFailingExpression());
		assertEquals(6.0, result.getCurrentValue(), 0);
		assertEquals(CheckResult.Status.WARN, result.getStatus());
	}

	@Test
	public void testAlertAfter2Failures() throws Exception {
		Check check = createCheckCheckingMean(2, 6);

		when(alertingPlugin.getChecks()).thenReturn(Collections.singletonMap(check.getId(), check));

		checkMetrics();
		verify(alerter, times(0)).alert(any(Alerter.AlertArguments.class));
		verify(incidentRepository).createIncident(any(Incident.class));

		checkMetrics();
		verify(alerter).alert(any(Alerter.AlertArguments.class));
		verify(incidentRepository).updateIncident(any(Incident.class));
	}

	@Test
	public void testNoAlertWhenFailureRecovers() throws Exception {
		Check check = createCheckCheckingMean(2, 6);
		when(alertingPlugin.getChecks()).thenReturn(Collections.singletonMap(check.getId(), check));

		// violation
		checkMetrics(7, 0, 0);
		verify(alerter, times(0)).alert(any(Alerter.AlertArguments.class));
		final Incident incident = incidentRepository.getIncidentByCheckId(check.getId());
		assertNotNull(incident);
		assertEquals(CheckResult.Status.OK, incident.getOldStatus());
		assertEquals(CheckResult.Status.WARN, incident.getNewStatus());
		assertNotNull(incident.getFirstFailureAt());
		assertNull(incident.getResolvedAt());
		assertEquals(1, incident.getConsecutiveFailures());
		System.out.println(incident);

		// back to ok
		checkMetrics(1, 0, 0);
		verify(alerter, times(0)).alert(any(Alerter.AlertArguments.class));
		assertNull(incidentRepository.getIncidentByCheckId(check.getId()));
	}

	@Test
	public void testAlertWhenBackToOk() throws Exception {
		Check check = createCheckCheckingMean(1, 6);
		when(alertingPlugin.getChecks()).thenReturn(Collections.singletonMap(check.getId(), check));

		// violation
		checkMetrics(7, 0, 0);
		verify(alerter, times(1)).alert(any(Alerter.AlertArguments.class));
		Incident incident = incidentRepository.getIncidentByCheckId(check.getId());
		assertNotNull(incident);
		assertEquals(CheckResult.Status.OK, incident.getOldStatus());
		assertEquals(CheckResult.Status.WARN, incident.getNewStatus());
		assertNotNull(incident.getFirstFailureAt());
		assertNull(incident.getResolvedAt());
		assertEquals(1, incident.getConsecutiveFailures());
		System.out.println(incident);

		// back to ok
		checkMetrics(1, 0, 0);
		ArgumentCaptor<Alerter.AlertArguments> alertArguments = ArgumentCaptor.forClass(Alerter.AlertArguments.class);
		verify(alerter, times(2)).alert(alertArguments.capture());
		incident = alertArguments.getValue().getIncident();
		assertNotNull(incident);
		assertEquals(CheckResult.Status.WARN, incident.getOldStatus());
		assertEquals(CheckResult.Status.OK, incident.getNewStatus());
		assertNotNull(incident.getFirstFailureAt());
		assertNotNull(incident.getResolvedAt());
	}

	@Test
	public void testDontDeleteIncidentIfThereAreNonOkResultsFromOtherInstances() {
		Check check = createCheckCheckingMean(2, 6);
		when(alertingPlugin.getChecks()).thenReturn(Collections.singletonMap(check.getId(), check));
		incidentRepository.createIncident(
				new Incident(check, new MeasurementSession("testApp", "testHost2", "testInstance"),
						Arrays.asList(new CheckResult("test", 10, CheckResult.Status.CRITICAL))));

		checkMetrics(7, 0, 0);
		verify(alerter, times(0)).alert(any(Alerter.AlertArguments.class));
		verify(incidentRepository).updateIncident(any(Incident.class));
		Incident storedIncident = incidentRepository.getIncidentByCheckId(check.getId());
		assertEquals(CheckResult.Status.CRITICAL, storedIncident.getOldStatus());
		assertEquals(CheckResult.Status.CRITICAL, storedIncident.getNewStatus());
		assertEquals(2, storedIncident.getCheckResults().size());
		assertEquals(1, storedIncident.getVersion());
		boolean containsTestHost = false;
		boolean containsTestHost2 = false;
		for (CheckResults checkResults : storedIncident.getCheckResults()) {
			if (checkResults.getMeasurementSession().getHostName().equals("testHost2")) {
				containsTestHost2 = true;
			} else if (checkResults.getMeasurementSession().getHostName().equals("testHost")) {
				containsTestHost = true;
			}
		}
		assertTrue(containsTestHost);
		assertTrue(containsTestHost2);
		System.out.println(storedIncident);
		System.out.println(JsonUtils.toJson(storedIncident));

		checkMetrics(1, 0, 0);
		verify(alerter, times(0)).alert(any(Alerter.AlertArguments.class));
		verify(incidentRepository, times(0)).deleteIncident(any(Incident.class));
		verify(incidentRepository, times(2)).updateIncident(any(Incident.class));

		storedIncident = incidentRepository.getIncidentByCheckId(check.getId());
		assertEquals(CheckResult.Status.CRITICAL, storedIncident.getOldStatus());
		assertEquals(CheckResult.Status.CRITICAL, storedIncident.getNewStatus());
		assertEquals(1, storedIncident.getCheckResults().size());
		assertEquals(2, storedIncident.getVersion());
	}

	public static Check createCheckCheckingMean(int alertAfterXFailures, long meanMs) {
		Check check = new Check();
		check.setName("Test Timer");
		check.setApplication("testApp");
		check.setTarget(name("test_timer").build());
		check.setMetricCategory(MetricCategory.TIMER);
		check.setAlertAfterXFailures(alertAfterXFailures);
		check.getWarn().add(new Threshold("mean", Threshold.Operator.GREATER_EQUAL, meanMs));
		return check;
	}

	private void checkMetrics() {
		checkMetrics(5, 4, 6);
	}

	private void checkMetrics(long timer1Mean, long timer2Mean, long timer3Mean) {
		final Map<MetricName, Timer> timers = new HashMap<>();
		timers.put(name("test_timer").tag("signature", "timer1").build(), timer(TimeUnit.MILLISECONDS.toNanos(timer1Mean)));
		timers.put(name("test_timer").tag("signature", "timer2").build(), timer(TimeUnit.MILLISECONDS.toNanos(timer2Mean)));
		timers.put(name("test_timer").tag("signature", "timer3").build(), timer(TimeUnit.MILLISECONDS.toNanos(timer3Mean)));
		timers.put(name("test_other_timer").tag("signature", "timer4").build(), timer(TimeUnit.MILLISECONDS.toNanos(999)));
		thresholdMonitoringReporter.reportMetrics(
				new HashMap<>(),
				new HashMap<>(),
				new HashMap<>(),
				new HashMap<>(),
				timers
		);
	}
}
