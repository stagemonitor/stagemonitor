package org.stagemonitor.alerting;

import org.junit.Test;
import org.stagemonitor.alerting.check.Threshold;
import org.stagemonitor.core.util.JsonUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThresholdTest {

	@Test
	public void testLess() throws Exception {
		Threshold threshold = new Threshold(Threshold.Operator.LESS, 0);
		assertTrue(threshold.isExceeded(-1));
		assertFalse(threshold.isExceeded(0));
		assertFalse(threshold.isExceeded(1));
	}

	@Test
	public void testLessEqual() throws Exception {
		Threshold threshold = new Threshold(Threshold.Operator.LESS_EQUAL, 0);
		assertTrue(threshold.isExceeded(-1));
		assertTrue(threshold.isExceeded(0));
		assertFalse(threshold.isExceeded(1));
	}

	@Test
	public void testGreater() throws Exception {
		Threshold threshold = new Threshold(Threshold.Operator.GREATER, 0);
		assertFalse(threshold.isExceeded(-1));
		assertFalse(threshold.isExceeded(0));
		assertTrue(threshold.isExceeded(1));
	}

	@Test
	public void testGreaterEqual() throws Exception {
		Threshold threshold = new Threshold(Threshold.Operator.GREATER_EQUAL, 0);
		assertFalse(threshold.isExceeded(-1));
		assertTrue(threshold.isExceeded(0));
		assertTrue(threshold.isExceeded(1));
	}

	@Test
	public void testJson() throws Exception {
		final String json = JsonUtils.toJson(new Threshold(Threshold.Operator.GREATER, 0));
		assertEquals(Threshold.Operator.GREATER, JsonUtils.getMapper().readValue(json, Threshold.class).getOperator());
	}
}
