package org.stagemonitor.alerting;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.stagemonitor.core.metrics.MetricsReporterTestHelper;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.map;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.timer;

public class ThresholdMonitoringReporterTest {

	private ThresholdMonitoringReporter thresholdMonitoringReporter;
	private CheckGroupRepository checkGroupRepository;
	private AlerterFactory alerterFactory;
	private Alerter alerter;

	@Before
	public void setUp() throws Exception {
		checkGroupRepository = mock(CheckGroupRepository.class);
		alerterFactory = mock(AlerterFactory.class);
		thresholdMonitoringReporter = new ThresholdMonitoringReporter(new MetricRegistry(), checkGroupRepository, alerterFactory);
		alerter = mock(Alerter.class);
		when(alerterFactory.getAlerters(Mockito.<Incident>any())).thenReturn(Arrays.asList(alerter));
	}

	@Test
	public void testAlerting() throws Exception {
		CheckGroup checkGroup = createCheckGroupCheckingMean(1, 5);

		when(checkGroupRepository.getAllActiveCheckGroups()).thenReturn(Arrays.asList(checkGroup));

		checkMetrics();

		ArgumentCaptor<Incident> incident = ArgumentCaptor.forClass(Incident.class);
		verify(alerter).alert(incident.capture());
		assertEquals(checkGroup.getId(), incident.getValue().getCheckGroupId());
		assertEquals("Test Timer", incident.getValue().getCheckGroupName());
		assertEquals(Check.Status.OK, incident.getValue().getOldStatus());
		assertEquals(Check.Status.WARN, incident.getValue().getNewStatus());

		List<Check.Result> checkResults = incident.getValue().getCheckResults();
		assertEquals(2, checkResults.size());

		Check.Result result = checkResults.get(0);
		assertEquals("test.timer1.mean >= 5.0", result.getFailingExpression());
		assertEquals(5.0, result.getCurrentValue(), 0);
		assertEquals(Check.Status.WARN, result.getStatus());
	}

	@Test
	public void testAlertAfter2Failures() throws Exception {
		CheckGroup checkGroup = createCheckGroupCheckingMean(2, 6);

		when(checkGroupRepository.getAllActiveCheckGroups()).thenReturn(Arrays.asList(checkGroup));

		checkMetrics();
		verify(alerter, times(0)).alert(any(Incident.class));

		checkMetrics();
		verify(alerter).alert(any(Incident.class));
	}

	@Test
	public void testNoAlertWhenFailureRecovers() throws Exception {
		CheckGroup checkGroup = createCheckGroupCheckingMean(2, 6);
		when(checkGroupRepository.getAllActiveCheckGroups()).thenReturn(Arrays.asList(checkGroup));

		checkMetrics(7, 0, 0);
		verify(alerter, times(0)).alert(any(Incident.class));

		checkMetrics(1, 0, 0);
		verify(alerter, times(0)).alert(any(Incident.class));
	}

	private CheckGroup createCheckGroupCheckingMean(int alertAfterXFailures, long meanMs) {
		CheckGroup checkGroup = new CheckGroup();
		checkGroup.setName("Test Timer");
		checkGroup.setTarget(Pattern.compile("test.timer.*"));
		checkGroup.setMetricCategory(MetricCategory.TIMER);
		checkGroup.setAlertAfterXFailures(alertAfterXFailures);
		checkGroup.setChecks(Arrays.asList(
				new Check("mean",
						new Threshold(Threshold.Operator.GREATER_EQUAL, meanMs), null, null)));
		return checkGroup;
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
