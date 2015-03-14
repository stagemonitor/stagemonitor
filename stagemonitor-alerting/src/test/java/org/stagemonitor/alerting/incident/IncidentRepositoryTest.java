package org.stagemonitor.alerting.incident;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.alerting.ThresholdMonitoringReporterTest;
import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.configuration.AbstractElasticsearchTest;
import org.stagemonitor.core.util.JsonUtils;

public class IncidentRepositoryTest extends AbstractElasticsearchTest {

	private Collection<IncidentRepository> incidentRepositorys;

	@Before
	public void setUp() throws Exception {
		incidentRepositorys = Arrays.asList(new ElasticsearchIncidentRepository(elasticsearchClient), new ConcurrentMapIncidentRepository());
	}

	@Test
	public void testSaveAndGet() throws Exception {
		for (IncidentRepository incidentRepository : incidentRepositorys) {
			Incident incident = createIncidentWithVersion("id1", 1);
			assertTrue(incidentRepository.createIncident(incident));
			refresh();
			assertIncidentEquals(incidentRepository.getIncidentByCheckId(incident.getCheckId()), incident);
			assertIncidentEquals(incidentRepository.getAllIncidents().iterator().next(), incident);
		}
	}

	@Test
	public void testGetNotPresent() throws Exception {
		for (IncidentRepository incidentRepository : incidentRepositorys) {
			assertNull(incidentRepository.getIncidentByCheckId("testGetNotPresent"));
			assertTrue(incidentRepository.getAllIncidents().isEmpty());
		}
	}

	@Test
	public void testAlreadyCreated() {
		for (IncidentRepository incidentRepository : incidentRepositorys) {
			assertTrue(incidentRepository.createIncident(createIncidentWithVersion("id1", 2)));
			assertFalse(incidentRepository.createIncident(createIncidentWithVersion("id1", 3)));
		}
	}

	@Test
	public void testWrongVersion() {
		for (IncidentRepository incidentRepository : incidentRepositorys) {
			assertTrue(incidentRepository.createIncident(createIncidentWithVersion("id1", 2)));
			assertFalse(incidentRepository.updateIncident(createIncidentWithVersion("id1", 2)));
			assertTrue(incidentRepository.updateIncident(createIncidentWithVersion("id1", 3)));
		}
	}

	@Test
	public void testDelete() throws Exception {
		for (IncidentRepository incidentRepository : incidentRepositorys) {
			assertTrue(incidentRepository.createIncident(createIncidentWithVersion("id1", 2)));
			assertTrue(incidentRepository.deleteIncident(createIncidentWithVersion("id1", 3)));
			assertNull(incidentRepository.getIncidentByCheckId("id1"));
			assertTrue(incidentRepository.getAllIncidents().isEmpty());
		}
	}

	@Test
	public void testDeleteWrongVersion() throws Exception {
		for (IncidentRepository incidentRepository : incidentRepositorys) {
			assertTrue(incidentRepository.createIncident(createIncidentWithVersion("id1", 2)));
			assertFalse(incidentRepository.deleteIncident(createIncidentWithVersion("id1", 2)));
			assertFalse(incidentRepository.deleteIncident(createIncidentWithVersion("id1", 1)));
		}
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
