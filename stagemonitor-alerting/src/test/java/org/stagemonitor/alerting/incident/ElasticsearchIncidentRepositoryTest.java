package org.stagemonitor.alerting.incident;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.AbstractElasticsearchTest;
import org.stagemonitor.alerting.ThresholdMonitoringReporterTest;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.core.MeasurementSession;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class ElasticsearchIncidentRepositoryTest extends AbstractElasticsearchTest {

	private ElasticsearchIncidentRepository elasticsearchIncidentRepository;

	@Before
	public void setUp() throws Exception {
		elasticsearchIncidentRepository = new ElasticsearchIncidentRepository(elasticsearchClient);
	}

	@Test
	public void testGetNonExistingIncindent() throws Exception {
		assertThat(elasticsearchIncidentRepository.getIncidentByCheckId("test 1")).isNull();
	}

	@Test
	public void testCreateAndGetIncindent() throws Exception {
		final Check check = ThresholdMonitoringReporterTest.createCheckCheckingMean(1, 5);
		check.setId("test 1");
		final Incident incident = new Incident(check,
				new MeasurementSession("testApp", "testHost2", "testInstance"),
				Arrays.asList(new CheckResult("test", 10, CheckResult.Status.CRITICAL),
						new CheckResult("test", 10, CheckResult.Status.ERROR)));
		elasticsearchIncidentRepository.createIncident(incident);
		refresh();
		assertThat(elasticsearchIncidentRepository.getIncidentByCheckId("test 1")).isEqualTo(incident);
	}
}
