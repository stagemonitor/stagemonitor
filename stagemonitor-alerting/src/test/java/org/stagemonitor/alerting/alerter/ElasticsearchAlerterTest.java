package org.stagemonitor.alerting.alerter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.AbstractElasticsearchTest;
import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.core.util.HttpClient;

import java.util.Collection;

public class ElasticsearchAlerterTest extends AbstractElasticsearchTest {

	public AbstractAlerterTest abstractAlerterTest = new AbstractAlerterTest();

	private ElasticsearchAlerter elasticsearchAlerter;
	private AlertSender alertSender;

	@Before
	public void setUp() throws Exception {
		abstractAlerterTest.configurationSource.add("stagemonitor.reporting.elasticsearch.url", elasticsearchUrl);
		abstractAlerterTest.configuration.reloadDynamicConfigurationOptions();
		elasticsearchAlerter = new ElasticsearchAlerter(abstractAlerterTest.configuration, new HttpClient());
		this.alertSender = abstractAlerterTest.createAlertSender(elasticsearchAlerter);
	}

	@Test
	public void testAlert() throws Exception {
		Incident incident = alertSender.sendTestAlert(abstractAlerterTest.createSubscription(elasticsearchAlerter), CheckResult.Status.ERROR);
		refresh();
		Collection<Incident> allIncidents = elasticsearchClient.getAll("/stagemonitor/alerts", 10, Incident.class);
		Assert.assertEquals(1, allIncidents.size());
		Assert.assertEquals(incident, allIncidents.iterator().next());
	}

}
