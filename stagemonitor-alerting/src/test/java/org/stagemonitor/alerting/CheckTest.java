package org.stagemonitor.alerting;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.alerting.check.MetricCategory;
import org.stagemonitor.alerting.check.Threshold;
import org.stagemonitor.core.util.JsonUtils;

public class CheckTest {

	private Check check;

	@Before
	public void setUp() {
		check = new Check();
		check.getCritical().add(new Threshold("value", Threshold.Operator.GREATER, 3));
		check.getError().add(new Threshold("value", Threshold.Operator.GREATER, 2));
		check.getWarn().add(new Threshold("value", Threshold.Operator.GREATER, 1));
	}

	@Test
	public void testCheckOK() throws Exception {
		assertEquals(0, check.check("test", singletonMap("value", 0d)).size());
	}

	@Test
	public void testCheckWarn() throws Exception {
		CheckResult result = check.check("test", singletonMap("value", 1.5)).iterator().next();
		assertEquals("test.value > 1.0", result.getFailingExpression());
		assertEquals(1.5, result.getCurrentValue(), 0);
		assertEquals(CheckResult.Status.WARN, result.getStatus());
	}

	@Test
	public void testCheckError() throws Exception {
		CheckResult result = check.check("test", singletonMap("value", 2.5)).iterator().next();
		assertEquals("test.value > 2.0", result.getFailingExpression());
		assertEquals(2.5, result.getCurrentValue(), 0);
		assertEquals(CheckResult.Status.ERROR, result.getStatus());
	}

	@Test
	public void testCheckCritical() throws Exception {
		CheckResult result = check.check("test", singletonMap("value", 3.5)).iterator().next();
		assertEquals("test.value > 3.0", result.getFailingExpression());
		assertEquals(3.5, result.getCurrentValue(), 0);
		assertEquals(CheckResult.Status.CRITICAL, result.getStatus());
	}

	@Test
	public void testGetMostSevereStatus() {
		assertEquals(CheckResult.Status.OK, CheckResult.getMostSevereStatus(Collections.<CheckResult>emptyList()));
	}

	@Test
	public void testJson() throws Exception {
		Check check = new Check();
		check.setName("Test Timer");
		check.setTarget(Pattern.compile("test.timer.*"));
		check.setMetricCategory(MetricCategory.TIMER);
		check.setAlertAfterXFailures(2);
		check.getWarn().add(new Threshold("mean", Threshold.Operator.GREATER_EQUAL, 3));

		final String json = JsonUtils.toJson(check);
		final Check checkFromJson = JsonUtils.getMapper().readValue(json, Check.class);
		assertEquals("Test Timer", checkFromJson.getName());
		assertEquals("test.timer.*", checkFromJson.getTarget().toString());
		assertEquals(MetricCategory.TIMER, checkFromJson.getMetricCategory());
		assertEquals(2, checkFromJson.getAlertAfterXFailures());
		assertEquals(1, checkFromJson.getWarn().size());
		assertEquals("mean", checkFromJson.getWarn().get(0).getMetric());
		assertEquals(Threshold.Operator.GREATER_EQUAL, checkFromJson.getWarn().get(0).getOperator());
		assertEquals(3, checkFromJson.getWarn().get(0).getThresholdValue(), 0);
		assertEquals(0, checkFromJson.getError().size());
		assertEquals(0, checkFromJson.getCritical().size());
		assertEquals(json, JsonUtils.toJson(checkFromJson));
	}
}
