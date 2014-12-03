package org.stagemonitor.alerting;

import com.fasterxml.jackson.core.JsonParseException;
import org.junit.Test;
import org.stagemonitor.core.util.JsonUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
	public void testAllExceeded() throws Exception {
		final List<Threshold> thresholds = Arrays.asList(new Threshold(Threshold.Operator.GREATER, 0), new Threshold(Threshold.Operator.GREATER, 2));
		assertTrue(Threshold.isAllExceeded(thresholds, 3));
		assertFalse(Threshold.isAllExceeded(thresholds, 2));
	}

	@Test
	public void testJson() throws Exception {
		final String json = JsonUtils.toJson(new Threshold(Threshold.Operator.GREATER, 0));
		assertEquals(Threshold.Operator.GREATER, JsonUtils.getMapper().readValue(json, Threshold.class).getOperator());
	}
}
