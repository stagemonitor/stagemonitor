package org.stagemonitor.alerting.alerter;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.core.configuration.AbstractElasticsearchTest;

public class HttpAlerterTest extends AbstractElasticsearchTest {

	public AbstractAlerterTest abstractAlerterTest = new AbstractAlerterTest();

	private HttpAlerter httpAlerter;
	private AlertSender alertSender;

	@Before
	public void setUp() throws Exception {
		abstractAlerterTest.configurationSource.add("stagemonitor.elasticsearch.url", elasticsearchUrl);
		abstractAlerterTest.configuration.reloadDynamicConfigurationOptions();
		httpAlerter = new HttpAlerter();
		this.alertSender = abstractAlerterTest.createAlertSender(httpAlerter);
	}

	@Test
	public void testAlert() throws Exception {
		Subscription subscription = abstractAlerterTest.createSubscription(httpAlerter);
		subscription.setTarget(elasticsearchUrl + "/stagemonitor/alerts");
		Incident incident = alertSender.sendTestAlert(subscription, CheckResult.Status.ERROR);
		refresh();
		Collection<Incident> allIncidents = elasticsearchClient.getAll("/stagemonitor/alerts", 10, Incident.class);
		Assert.assertEquals(1, allIncidents.size());
		Assert.assertEquals(incident, allIncidents.iterator().next());
	}
}
