package org.stagemonitor.core.metrics.metrics2;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

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

	@Test
	public void testTemplateSingleValue() {
		final MetricName.MetricNameTemplate metricNameTemplate = name("foo").tag("bar", "").tag("qux", "quux").templateFor("bar");
		assertEquals(name("foo").tag("bar", "baz").tag("qux", "quux").build(), metricNameTemplate.build("baz"));
		assertSame(metricNameTemplate.build("baz"), metricNameTemplate.build("baz"));
		assertNotEquals(metricNameTemplate.build("baz"), metricNameTemplate.build("baz2"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTemplateSingleValueBuildMultipleValues() {
		final MetricName.MetricNameTemplate metricNameTemplate = name("foo").tag("bar", "").tag("qux", "quux").templateFor("bar");
		metricNameTemplate.build("foo", "bar");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTemplateSingleValueEmptyValues() {
		final MetricName.MetricNameTemplate metricNameTemplate = name("foo").tag("bar", "").tag("qux", "quux").templateFor("bar");
		metricNameTemplate.build();
	}

	@Test(expected = NullPointerException.class)
	public void testTemplateSingleValueNull() {
		final MetricName.MetricNameTemplate metricNameTemplate = name("foo").tag("bar", "").tag("qux", "quux").templateFor("bar");
		metricNameTemplate.build((String) null);
	}

	@Test
	public void testTemplateMultipleValues() {
		final MetricName.MetricNameTemplate metricNameTemplate = name("foo").tag("bar", "").tag("qux", "quux").templateFor("bar", "qux");
		assertEquals(name("foo").tag("bar", "baz").tag("qux", "q").build(), metricNameTemplate.build("baz", "q"));
		assertSame(metricNameTemplate.build("baz", "quux"), metricNameTemplate.build("baz", "quux"));
		assertNotEquals(metricNameTemplate.build("baz", "quux"), metricNameTemplate.build("baz2", "quux"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTemplateMultipleValuesBuildEmptyValues() {
		final MetricName.MetricNameTemplate metricNameTemplate = name("foo").tag("bar", "").tag("qux", "quux").templateFor("bar", "qux");
		metricNameTemplate.build("foo");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTemplateMultipleValuesBuildTooFewValues() {
		final MetricName.MetricNameTemplate metricNameTemplate = name("foo").tag("bar", "").tag("qux", "quux").templateFor("bar", "qux");
		metricNameTemplate.build("foo");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTemplateMultipleValuesBuildTooManyValues() {
		final MetricName.MetricNameTemplate metricNameTemplate = name("foo").tag("bar", "").tag("qux", "quux").templateFor("bar", "qux");
		metricNameTemplate.build("foo", "bar", "baz");
	}

	@Test(expected = NullPointerException.class)
	public void testMetricNameNull() {
		name("foo").tag("bar", null).tag("qux", null).build();
	}

	@Test(expected = NullPointerException.class)
	public void testTemplateMultipleValuesNull() {
		final MetricName.MetricNameTemplate metricNameTemplate = name("foo").tag("bar", "").tag("qux", "quux").templateFor("bar", "qux");
		metricNameTemplate.build(null, null);
	}
}