package org.stagemonitor.alerting.incident;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.alerting.ThresholdMonitoringReporterTest;
import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.configuration.AbstractElasticsearchTest;
import org.stagemonitor.core.util.JsonUtils;

public class IncidentRepositoryTest extends AbstractElasticsearchTest {

	private IncidentRepository incidentRepository;

	@Before
	public void setUp() throws Exception {
		incidentRepository = new ElasticsearchIncidentRepository(elasticsearchClient);
	}

	@Test
	public void testSaveAndGet() throws Exception {
		Incident incident = new Incident(ThresholdMonitoringReporterTest.createCheckCheckingMean(1, 5),
				new MeasurementSession("testApp", "testHost2", "testInstance"),
				Arrays.asList(new CheckResult("test", 10, CheckResult.Status.CRITICAL)));

		incidentRepository.createIncident(incident);
		refresh();
		assertEquals(JsonUtils.toJson(incidentRepository.getIncidentByCheckId(incident.getCheckId())), JsonUtils.toJson(incident));
		assertEquals(JsonUtils.toJson(incidentRepository.getAllIncidents().iterator().next()), JsonUtils.toJson(incident));
	}

}
