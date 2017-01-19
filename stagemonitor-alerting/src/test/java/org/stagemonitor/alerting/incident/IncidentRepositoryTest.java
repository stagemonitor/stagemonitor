package org.stagemonitor.alerting.incident;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.stagemonitor.alerting.ThresholdMonitoringReporterTest;
import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.configuration.AbstractElasticsearchTest;
import org.stagemonitor.core.util.JsonUtils;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class IncidentRepositoryTest<T extends IncidentRepository> extends AbstractElasticsearchTest {

	private final T incidentRepository;

	public IncidentRepositoryTest(T incidentRepository, Class<T> clazz) {
		this.incidentRepository = incidentRepository;
		if (incidentRepository instanceof ElasticsearchIncidentRepository) {
			final ElasticsearchIncidentRepository elasticsearchIncidentRepository = (ElasticsearchIncidentRepository) incidentRepository;
			elasticsearchIncidentRepository.setElasticsearchClient(elasticsearchClient);
		}
	}

	@Before
	public void setUp() throws Exception {
		incidentRepository.clear();
	}

	@Parameterized.Parameters(name = "{index}: {1}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{new ElasticsearchIncidentRepository(elasticsearchClient), ElasticsearchIncidentRepository.class},
				{new ConcurrentMapIncidentRepository(), ConcurrentMapIncidentRepository.class}
		});
	}

	@Test
	public void testSaveAndGet() throws Exception {
		Incident incident = createIncidentWithVersion("id1", 1);
		assertTrue(incidentRepository.createIncident(incident));
		refresh();
		assertIncidentEquals(incidentRepository.getIncidentByCheckId(incident.getCheckId()), incident);
		assertIncidentEquals(incidentRepository.getAllIncidents().iterator().next(), incident);
	}

	@Test
	public void testGetNotPresent() throws Exception {
		assertNull(incidentRepository.getIncidentByCheckId("testGetNotPresent"));
		assertTrue(incidentRepository.getAllIncidents().isEmpty());
	}

	@Test
	public void testAlreadyCreated() {
		assertTrue(incidentRepository.createIncident(createIncidentWithVersion("id1", 1)));
		assertFalse(incidentRepository.createIncident(createIncidentWithVersion("id1", 1)));
	}

	@Test
	public void testWrongVersion() {
		assertTrue(incidentRepository.createIncident(createIncidentWithVersion("id1", 1)));
		assertFalse(incidentRepository.updateIncident(createIncidentWithVersion("id1", 1)));
		assertTrue(incidentRepository.updateIncident(createIncidentWithVersion("id1", 2)));
	}

	@Test
	public void testDelete() throws Exception {
		assertTrue(incidentRepository.createIncident(createIncidentWithVersion("id1", 1)));
		assertTrue(incidentRepository.deleteIncident(createIncidentWithVersion("id1", 2)));
		assertNull(incidentRepository.getIncidentByCheckId("id1"));
		assertTrue(incidentRepository.getAllIncidents().isEmpty());
	}

	@Test
	public void testDeleteWrongVersion() throws Exception {
		assertTrue(incidentRepository.createIncident(createIncidentWithVersion("id1", 1)));
		assertFalse(incidentRepository.deleteIncident(createIncidentWithVersion("id1", 1)));
		assertFalse(incidentRepository.deleteIncident(createIncidentWithVersion("id1", 0)));
	}

	private void assertIncidentEquals(Incident expected, Incident actual) {
		assertEquals(JsonUtils.toJson(expected), JsonUtils.toJson(actual));
	}

	public static Incident createIncidentWithVersion(String checkId, int version) {
		Incident incident = new Incident(ThresholdMonitoringReporterTest.createCheckCheckingMean(1, 5),
				new MeasurementSession("testApp", "testHost2", "testInstance"),
				Arrays.asList(new CheckResult("test", 10, CheckResult.Status.CRITICAL),
						new CheckResult("test", 10, CheckResult.Status.ERROR)));
		incident.setVersion(version);
		incident.setCheckId(checkId);
		return incident;
	}

}
