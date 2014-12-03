package org.stagemonitor.alerting;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.util.JsonUtils;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

public class CheckTest {

	private Check check;

	@Before
	public void setUp() {
		check = new Check(Pattern.compile("test"), "test");
		check.getCritical().add(new Threshold(Threshold.Operator.GREATER, 3));
		check.getError().add(new Threshold(Threshold.Operator.GREATER, 2));
		check.getWarn().add(new Threshold(Threshold.Operator.GREATER, 1));
	}

	@Test
	public void testCheckOK() throws Exception {
		assertEquals(Check.Status.OK, check.check(0));
	}

	@Test
	public void testCheckWarn() throws Exception {
		assertEquals(Check.Status.WARN, check.check(1.5));
	}

	@Test
	public void testCheckError() throws Exception {
		assertEquals(Check.Status.ERROR, check.check(2.5));
	}

	@Test
	public void testCheckCritical() throws Exception {
		assertEquals(Check.Status.CRITICAL, check.check(3.5));
	}

	@Test
	public void testJson() throws Exception {
		final String json = JsonUtils.toJson(new Check(Pattern.compile("requests.([^\\.]+).time"), "p95"));
		assertEquals("requests.([^\\.]+).time", JsonUtils.getMapper().readValue(json, Check.class).getMetricName().toString());
	}
}
