package org.stagemonitor.alerting;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CheckTest {

	private Check check;

	@Before
	public void setUp() {
		check = new Check();
		check.setMetric("value");
		check.setCritical(new Threshold(Threshold.Operator.GREATER, 3));
		check.setError(new Threshold(Threshold.Operator.GREATER, 2));
		check.setWarn(new Threshold(Threshold.Operator.GREATER, 1));
	}

	@Test
	public void testCheckOK() throws Exception {
		Check.Result result = check.check("test", 0);
		assertNull(result.getFailingExpression());
		assertEquals(0, result.getCurrentValue(), 0);
		assertEquals(Check.Status.OK, result.getStatus());
	}

	@Test
	public void testCheckWarn() throws Exception {
		Check.Result result = check.check("test", 1.5);
		assertEquals("test.value > 1.0", result.getFailingExpression());
		assertEquals(1.5, result.getCurrentValue(), 0);
		assertEquals(Check.Status.WARN, result.getStatus());
	}

	@Test
	public void testCheckError() throws Exception {
		Check.Result result = check.check("test", 2.5);
		assertEquals("test.value > 2.0", result.getFailingExpression());
		assertEquals(2.5, result.getCurrentValue(), 0);
		assertEquals(Check.Status.ERROR, result.getStatus());
	}

	@Test
	public void testCheckCritical() throws Exception {
		Check.Result result = check.check("test", 3.5);
		assertEquals("test.value > 3.0", result.getFailingExpression());
		assertEquals(3.5, result.getCurrentValue(), 0);
		assertEquals(Check.Status.CRITICAL, result.getStatus());
	}

	@Test
	public void testGetResultsWithMostSevereStatus() {
		List<Check.Result> resultsWithMostSevereStatus = Check.Result.getResultsWithMostSevereStatus(
				Arrays.asList(
						new Check.Result(null, 0, Check.Status.OK),
						new Check.Result(null, 0, Check.Status.WARN),
						new Check.Result(null, 0, Check.Status.ERROR),
						new Check.Result("a", 0, Check.Status.CRITICAL),
						new Check.Result("b", 0, Check.Status.CRITICAL)
				)
		);

		assertEquals(2, resultsWithMostSevereStatus.size());
		assertEquals(Check.Status.CRITICAL, resultsWithMostSevereStatus.get(0).getStatus());
		assertEquals("a", resultsWithMostSevereStatus.get(0).getFailingExpression());
		assertEquals(Check.Status.CRITICAL, resultsWithMostSevereStatus.get(1).getStatus());
		assertEquals("b", resultsWithMostSevereStatus.get(1).getFailingExpression());
	}

//	@Test
//	public void testJson() throws Exception {
//		final String json = JsonUtils.toJson(new Check(Pattern.compile("requests.([^\\.]+).time"), "p95"));
//		assertEquals("requests.([^\\.]+).time", JsonUtils.getMapper().readValue(json, Check.class).getTarget().toString());
//	}
}
