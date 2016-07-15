package org.stagemonitor.alerting;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.alerting.check.MetricValueType;
import org.stagemonitor.alerting.check.Threshold;
import org.stagemonitor.core.util.JsonUtils;

import java.util.Collections;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class CheckTest {

	private Check check;

	@Before
	public void setUp() {
		check = new Check();
		check.getCritical().add(new Threshold("value", Threshold.Operator.LESS_EQUAL, 3));
		check.getError().add(new Threshold("value", Threshold.Operator.LESS_EQUAL, 2));
		check.getWarn().add(new Threshold("value", Threshold.Operator.LESS_EQUAL, 1));
	}

	@Test
	public void testCheckOK() throws Exception {
		assertEquals(0, check.check(name("test").build(), singletonMap("value", 0d)).size());
	}

	@Test
	public void testCheckWarn() throws Exception {
		CheckResult result = check.check(name("test").build(), singletonMap("value", 1.5)).iterator().next();
		assertEquals("test value <= 1.0 is false", result.getFailingExpression());
		assertEquals(1.5, result.getCurrentValue(), 0);
		assertEquals(CheckResult.Status.WARN, result.getStatus());
	}

	@Test
	public void testCheckError() throws Exception {
		CheckResult result = check.check(name("test").build(), singletonMap("value", 2.5)).iterator().next();
		assertEquals("test value <= 2.0 is false", result.getFailingExpression());
		assertEquals(2.5, result.getCurrentValue(), 0);
		assertEquals(CheckResult.Status.ERROR, result.getStatus());
	}

	@Test
	public void testCheckCritical() throws Exception {
		CheckResult result = check.check(name("test").build(), singletonMap("value", 3.5)).iterator().next();
		assertEquals("test value <= 3.0 is false", result.getFailingExpression());
		assertEquals(3.5, result.getCurrentValue(), 0);
		assertEquals(CheckResult.Status.CRITICAL, result.getStatus());
	}
	@Test
	public void testCheckCriticalFromJson() throws Exception {
		Check checkFromJson = JsonUtils.getMapper().readValue("{\"id\":\"50d3063f-437f-431c-bbf5-601ea0943cdf\"," +
				"\"name\":null," +
				"\"target\":null," +
				"\"alertAfterXFailures\":1," +
				"\"thresholds\":{" +
				"\"ERROR\":[{\"valueType\":\"VALUE\",\"operator\":\"LESS_EQUAL\",\"thresholdValue\":2.0}]," +
				// WARN is not in list
				// CRITICAL is last in list
				"\"CRITICAL\":[{\"valueType\":\"VALUE\",\"operator\":\"LESS_EQUAL\",\"thresholdValue\":3.0}]" +
				"}," +
				"\"application\":null," +
				"\"active\":true}", Check.class);
		CheckResult result = checkFromJson.check(name("test").build(), singletonMap("value", 3.5)).iterator().next();
		assertEquals(CheckResult.Status.CRITICAL, result.getStatus());
		assertEquals("test value <= 3.0 is false", result.getFailingExpression());
		assertEquals(3.5, result.getCurrentValue(), 0);
	}

	@Test
	public void testGetMostSevereStatus() {
		assertEquals(CheckResult.Status.OK, CheckResult.getMostSevereStatus(Collections.<CheckResult>emptyList()));
	}

	@Test
	public void testJson() throws Exception {
		Check check = new Check();
		check.setName("Test Timer");
		check.setTarget(name("timer").tag("foo", "bar").tag("qux", "quux").build());
		check.setAlertAfterXFailures(2);
		check.getWarn().add(new Threshold("mean", Threshold.Operator.GREATER_EQUAL, 3));

		final String json = JsonUtils.toJson(check);
		final Check checkFromJson = JsonUtils.getMapper().readValue(json, Check.class);
		assertEquals("Test Timer", checkFromJson.getName());
		assertEquals(name("timer").tag("foo", "bar").tag("qux", "quux").build(), checkFromJson.getTarget());
		assertEquals(2, checkFromJson.getAlertAfterXFailures());
		assertEquals(1, checkFromJson.getWarn().size());
		assertEquals(MetricValueType.MEAN, checkFromJson.getWarn().get(0).getValueType());
		assertEquals(Threshold.Operator.GREATER_EQUAL, checkFromJson.getWarn().get(0).getOperator());
		assertEquals(3, checkFromJson.getWarn().get(0).getThresholdValue(), 0);
		assertEquals(0, checkFromJson.getError().size());
		assertEquals(0, checkFromJson.getCritical().size());
		assertEquals(json, JsonUtils.toJson(checkFromJson));
	}
}
