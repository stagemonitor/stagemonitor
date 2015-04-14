package org.stagemonitor.alerting.alerter;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.slf4j.Logger;
import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.alerting.incident.Incident;

public class LogAlerterTest extends AbstractAlerterTest {

	private LogAlerter logAlerter;
	private Logger logger;
	private AlertSender alertSender;

	public LogAlerterTest() {
		super();
		logger = mock(Logger.class);
		logAlerter = new LogAlerter(alertingPlugin, logger);
		this.alertSender = createAlertSender(logAlerter);
	}

	@Test
	public void testLogAlert() throws Exception {
		Incident incident = alertSender.sendTestAlert(createSubscription(logAlerter), CheckResult.Status.ERROR);

		verify(logger).error(eq(String.format("Incident for check 'Test Check':\n" +
				"First failure: %s\n" +
				"Old status: OK\n" +
				"New status: ERROR\n" +
				"Failing check: 1\n" +
				"Hosts: testHost\n" +
				"Instances: testInstance\n" +
				"\n" +
				"Details:\n" +
				"Host:\t\t\ttestHost\n" +
				"Instance:\t\ttestInstance\n" +
				"Status: \t\tERROR\n" +
				"Description:\ttest\n" +
				"Current value:\t10\n" +
				"\n", toFreemarkerIsoLocal(incident.getFirstFailureAt()))));
	}
}
