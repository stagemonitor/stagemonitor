package org.stagemonitor.alerting.alerter;

import static org.junit.Assert.assertEquals;
import static org.stagemonitor.alerting.incident.IncidentRepositoryTest.createIncidentWithVersion;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.alerting.incident.Incident;

public class AlertTemplateProcessorTest extends AbstractAlerterTest {

	private AlertTemplateProcessor alertTemplateProcessor;
	private Incident testIncident;

	@Before
	public void setUp() throws Exception {
		alertTemplateProcessor = alertingPlugin.getAlertTemplateProcessor();
		testIncident = createIncidentWithVersion("1", 1);
	}

	@Test
	public void testProcessShortDescriptionTemplate() throws Exception {
		assertEquals("[CRITICAL] Test Timer has 2 failing checks", alertTemplateProcessor.processShortDescriptionTemplate(createIncidentWithVersion("1", 1)));

		// modify template
		configurationSource.add("stagemonitor.alerts.template.shortDescription", "foo");
		configuration.reloadDynamicConfigurationOptions();
		assertEquals("foo", alertTemplateProcessor.processShortDescriptionTemplate(testIncident));
	}

	@Test
	public void testProcessPlainTextTemplate() throws Exception {
		assertEquals(String.format("Incident for check 'Test Timer':\n" +
				"First failure: %s\n" +
				"Old status: OK\n" +
				"New status: CRITICAL\n" +
				"Failing checks: 2\n" +
				"Hosts: testHost2\n" +
				"Instances: testInstance\n" +
				"\n" +
				"host|instance|status|description|current value\n" +
				"----|--------|------|-----------|-------------\n" +
				"testHost2 | testInstance | CRITICAL | test | 10\n" +
				"testHost2 | testInstance | ERROR | test | 10\n", toFreemarkerIsoLocal(testIncident.getFirstFailureAt())),
				alertTemplateProcessor.processPlainTextTemplate(testIncident));
	}

	@Test
	public void testProcessHtmlTemplate() throws Exception {
		assertEquals(String.format("<html>\n" +
				"<head>\n" +
				"\n" +
				"</head>\n" +
				"<body>\n" +
				"<h3>Incident for check Test Timer</h3>\n" +
				"First failure: %s<br>\n" +
				"Old status: OK<br>\n" +
				"New status: CRITICAL<br>\n" +
				"Failing checks: 2<br>\n" +
				"Hosts: testHost2<br>\n" +
				"Instances: testInstance<br><br>\n" +
				"\n" +
				"<table>\n" +
				"\t<thead>\n" +
				"\t<tr>\n" +
				"\t\t<th>Host</th>\n" +
				"\t\t<th>Instance</th>\n" +
				"\t\t<th>Status</th>\n" +
				"\t\t<th>Description</th>\n" +
				"\t\t<th>Current Value</th>\n" +
				"\t</tr>\n" +
				"\t</thead>\n" +
				"\t<tbody>\n" +
				"\t\t\t<tr>\n" +
				"\t\t\t\t<td>testHost2</td>\n" +
				"\t\t\t\t<td>testInstance</td>\n" +
				"\t\t\t\t<td>CRITICAL</td>\n" +
				"\t\t\t\t<td>test</td>\n" +
				"\t\t\t\t<td>10</td>\n" +
				"\t\t\t</tr>\n" +
				"\t\t\t<tr>\n" +
				"\t\t\t\t<td>testHost2</td>\n" +
				"\t\t\t\t<td>testInstance</td>\n" +
				"\t\t\t\t<td>ERROR</td>\n" +
				"\t\t\t\t<td>test</td>\n" +
				"\t\t\t\t<td>10</td>\n" +
				"\t\t\t</tr>\n" +
				"\t</tbody>\n" +
				"</table>\n" +
				"</body>\n" +
				"</html>\n", toFreemarkerIsoLocal(testIncident.getFirstFailureAt())),
				alertTemplateProcessor.processHtmlTemplate(testIncident));

	}
}
