package org.stagemonitor.alerting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.map;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.timer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.stagemonitor.alerting.alerter.Alerter;
import org.stagemonitor.alerting.alerter.AlerterFactory;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.alerting.check.MetricCategory;
import org.stagemonitor.alerting.check.Threshold;
import org.stagemonitor.alerting.incident.ConcurrentMapIncidentRepository;
import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.alerting.incident.IncidentRepository;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.metrics.MetricsReporterTestHelper;

public class ThresholdMonitoringReporterTest {

	private final MeasurementSession measurementSession = new MeasurementSession("testApp", "testHost", "testInstance");
	private ThresholdMonitoringReporter thresholdMonitoringReporter;
	private Alerter alerter;
	private IncidentRepository incidentRepository;
	private AlertingPlugin alertingPlugin;

	@Before
	public void setUp() throws Exception {
		AlerterFactory alerterFactory = mock(AlerterFactory.class);
		alertingPlugin = mock(AlertingPlugin.class);
		incidentRepository = spy(new ConcurrentMapIncidentRepository(new ConcurrentHashMap<String, Incident>()));
		thresholdMonitoringReporter = new ThresholdMonitoringReporter(new MetricRegistry(), alertingPlugin,
				alerterFactory, incidentRepository, measurementSession);
		alerter = mock(Alerter.class);
		when(alerterFactory.getAlerters(Mockito.<Check>any(), Mockito.<Incident>any())).thenReturn(Arrays.asList(alerter));
	}

	@Test
	public void testAlerting() throws Exception {
		Check check = createCheckGroupCheckingMean(1, 5);

		when(alertingPlugin.getChecks()).thenReturn(Arrays.asList(check));

		checkMetrics();

		ArgumentCaptor<Incident> incident = ArgumentCaptor.forClass(Incident.class);
		verify(alerter).alert(incident.capture());
		assertEquals(check.getId(), incident.getValue().getCheckId());
		assertEquals("Test Timer", incident.getValue().getCheckName());
		assertEquals(CheckResult.Status.OK, incident.getValue().getOldStatus());
		assertEquals(CheckResult.Status.WARN, incident.getValue().getNewStatus());

		List<CheckResult> checkResults = incident.getValue().getResultsByHostAndInstance().get("testHost").getResultsByInstance("testInstance").getResults();
		assertEquals(2, checkResults.size());

		CheckResult result = checkResults.get(0);
		assertEquals("test.timer1.mean >= 5.0", result.getFailingExpression());
		assertEquals(5.0, result.getCurrentValue(), 0);
		assertEquals(CheckResult.Status.WARN, result.getStatus());
	}

	@Test
	public void testAlertAfter2Failures() throws Exception {
		Check check = createCheckGroupCheckingMean(2, 6);

		when(alertingPlugin.getChecks()).thenReturn(Arrays.asList(check));

		checkMetrics();
		verify(alerter, times(0)).alert(any(Incident.class));
		verify(incidentRepository).createIncident(any(Incident.class));

		checkMetrics();
		verify(alerter).alert(any(Incident.class));
		verify(incidentRepository).updateIncident(any(Incident.class));
	}

	@Test
	public void testNoAlertWhenFailureRecovers() throws Exception {
		Check check = createCheckGroupCheckingMean(2, 6);
		when(alertingPlugin.getChecks()).thenReturn(Arrays.asList(check));

		checkMetrics(7, 0, 0);
		verify(alerter, times(0)).alert(any(Incident.class));
		final Incident incident = incidentRepository.getIncidentByCheckGroupId(check.getId());
		assertNotNull(incident);
		assertEquals(CheckResult.Status.OK, incident.getOldStatus());
		assertEquals(CheckResult.Status.WARN, incident.getNewStatus());
		assertNotNull(incident.getFirstFailAt());
		assertNull(incident.getResolvedAt());
		assertEquals(1, incident.getConsecutiveFailures());
		System.out.println(incident);

		checkMetrics(1, 0, 0);
		verify(alerter, times(0)).alert(any(Incident.class));
		assertNull(incidentRepository.getIncidentByCheckGroupId(check.getId()));
	}

	@Test
	public void testDontDeleteIncidentIfThereAreNonOkResultsFromOtherInstances() {
		Check check = createCheckGroupCheckingMean(2, 6);
		when(alertingPlugin.getChecks()).thenReturn(Arrays.asList(check));
		incidentRepository.createIncident(
				new Incident(check, new MeasurementSession("testApp", "testHost2", "testInstance"),
						Arrays.asList(new CheckResult("test", 10, CheckResult.Status.CRITICAL))));

		checkMetrics(7, 0, 0);
		verify(alerter, times(0)).alert(any(Incident.class));
		verify(incidentRepository).updateIncident(any(Incident.class));
		Incident storedIncident = incidentRepository.getIncidentByCheckGroupId(check.getId());
		assertEquals(CheckResult.Status.CRITICAL, storedIncident.getOldStatus());
		assertEquals(CheckResult.Status.CRITICAL, storedIncident.getNewStatus());
		assertEquals(1, storedIncident.getResultsByHostAndInstance().get("testHost").getResultsByInstance("testInstance").getResults().size());
		assertEquals(1, storedIncident.getResultsByHostAndInstance().get("testHost2").getResultsByInstance("testInstance").getResults().size());
		System.out.println(storedIncident);

		checkMetrics(1, 0, 0);
		verify(alerter, times(0)).alert(any(Incident.class));
		verify(incidentRepository, times(0)).deleteIncident(any(Incident.class));
		verify(incidentRepository, times(2)).updateIncident(any(Incident.class));

		storedIncident = incidentRepository.getIncidentByCheckGroupId(check.getId());
		assertEquals(CheckResult.Status.CRITICAL, storedIncident.getOldStatus());
		assertEquals(CheckResult.Status.CRITICAL, storedIncident.getNewStatus());
		assertNull(storedIncident.getResultsByHostAndInstance().get("testHost"));
		assertEquals(1, storedIncident.getResultsByHostAndInstance().get("testHost2").getResultsByInstance("testInstance").getResults().size());
	}

	private Check createCheckGroupCheckingMean(int alertAfterXFailures, long meanMs) {
		Check check = new Check();
		check.setName("Test Timer");
		check.setApplication("testApp");
		check.setTarget(Pattern.compile("test.timer.*"));
		check.setMetricCategory(MetricCategory.TIMER);
		check.setAlertAfterXFailures(alertAfterXFailures);
		check.getWarn().add(new Threshold("mean", Threshold.Operator.GREATER_EQUAL, meanMs));
		return check;
	}

	private void checkMetrics() {
		checkMetrics(5, 4, 6);
	}

	private void checkMetrics(long timer1Mean, long timer2Mean, long timer3Mean) {
		thresholdMonitoringReporter.report(
				MetricsReporterTestHelper.<Gauge>map(),
				MetricsReporterTestHelper.<Counter>map(),
				MetricsReporterTestHelper.<Histogram>map(),
				MetricsReporterTestHelper.<Meter>map(),
				map("test.timer1", timer(TimeUnit.MILLISECONDS.toNanos(timer1Mean)))
						.add("test.timer2", timer(TimeUnit.MILLISECONDS.toNanos(timer2Mean)))
						.add("test.timer3", timer(TimeUnit.MILLISECONDS.toNanos(timer3Mean)))
						.add("test.some.other.timer", timer(TimeUnit.MILLISECONDS.toNanos(999)))
		);
	}
}
