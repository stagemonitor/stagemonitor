package org.stagemonitor.core.metrics.metrics2;

import static org.junit.Assert.assertEquals;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import org.junit.Test;

public class MetricNameTest {

	@Test
	public void testEquals() {
		assertEquals(name("foo").tag("bar", "baz").tag("qux", "quux").build(), name("foo").tag("bar", "baz").tag("qux", "quux").build());
		assertEquals(name("foo").tag("qux", "quux").tag("bar", "baz").build(), name("foo").tag("bar", "baz").tag("qux", "quux").build());
	}

	@Test
	public void testHashCode() {
		assertEquals(name("foo").tag("bar", "baz").tag("qux", "quux").build().hashCode(), name("foo").tag("bar", "baz").tag("qux", "quux").build().hashCode());
		assertEquals(name("foo").tag("qux", "quux").tag("bar", "baz").build().hashCode(), name("foo").tag("bar", "baz").tag("qux", "quux").build().hashCode());
	}
}