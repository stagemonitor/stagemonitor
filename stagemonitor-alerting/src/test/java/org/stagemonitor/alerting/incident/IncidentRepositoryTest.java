package org.stagemonitor.alerting.incident;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.AbstractElasticsearchTest;

public class IncidentRepositoryTest extends AbstractElasticsearchTest {

	private IncidentRepository incidentRepository;

	@BeforeClass
	public static void setup() throws Exception {
		refresh();
	}

	@AfterClass
	public static void reset() {
		Stagemonitor.reset();
	}

	@Before
	public void setUp() throws Exception {
//		configurationSource = new ElasticsearchConfigurationSource("test");
	}


}
