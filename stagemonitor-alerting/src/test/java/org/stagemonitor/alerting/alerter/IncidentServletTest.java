package org.stagemonitor.alerting.alerter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.ThresholdMonitoringReporterTest;
import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.alerting.incident.ConcurrentMapIncidentRepository;
import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.util.JsonUtils;

public class IncidentServletTest {

	private IncidentServlet incidentServlet;
	private Incident incident;
	private AlertingPlugin alertingPlugin;

	@Before
	public void setUp() throws Exception {
		alertingPlugin = mock(AlertingPlugin.class);
		ConcurrentMapIncidentRepository incidentRepository = new ConcurrentMapIncidentRepository();
		incident = new Incident(ThresholdMonitoringReporterTest.createCheckCheckingMean(1, 5),
				new MeasurementSession("testApp", "testHost2", "testInstance"),
				Arrays.asList(new CheckResult("test", 10, CheckResult.Status.CRITICAL),
						new CheckResult("test", 10, CheckResult.Status.ERROR)));
		incidentRepository.createIncident(incident);
		when(alertingPlugin.getIncidentRepository()).thenReturn(incidentRepository);
		incidentServlet = new IncidentServlet(alertingPlugin);
	}

	@Test
	public void testGetIncidents() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		incidentServlet.service(new MockHttpServletRequest("GET", "/stagemonitor/incidents"), response);
		String expected = JsonUtils.toJson(new HashMap<String, Object>() {{
			put("status", CheckResult.Status.CRITICAL);
			put("incidents", Arrays.asList(incident));
		}});
		Assert.assertEquals(expected, response.getContentAsString());
	}

	@Test
	public void testGetIncidentsIncidentRepositoryNull() throws Exception {
		when(alertingPlugin.getIncidentRepository()).thenReturn(null);

		MockHttpServletResponse response = new MockHttpServletResponse();
		incidentServlet.service(new MockHttpServletRequest("GET", "/stagemonitor/incidents"), response);
		Assert.assertEquals("{}", response.getContentAsString());
	}
}
