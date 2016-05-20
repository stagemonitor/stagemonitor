package org.stagemonitor.core.metrics.metrics2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import org.junit.Test;

public class MetricNameTest {

	@Test
	public void testEquals() {
		assertEquals(name("foo").tag("bar", "baz").tag("qux", "quux").build(), name("foo").tag("bar", "baz").tag("qux", "quux").build());
		assertNotEquals(name("foo").tag("qux", "quux").tag("bar", "baz").build(), name("foo").tag("bar", "baz").tag("qux", "quux").build());
	}
}