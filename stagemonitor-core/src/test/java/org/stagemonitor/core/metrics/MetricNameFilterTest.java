package org.stagemonitor.core.metrics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import com.codahale.metrics.Metric;
import org.junit.Test;
import org.mockito.Mockito;

public class MetricNameFilterTest {
	
	private final Metric mockMetric = Mockito.mock(Metric.class);

	@Test
	public void testNoTags() {
		MetricNameFilter includeFilter = MetricNameFilter.includePatterns(name("foo").build());

		assertTrue(includeFilter.matches(name("foo").build(), mockMetric));
		assertTrue(includeFilter.matches(name("foo").tag("bar", "baz").build(), mockMetric));
		assertFalse(includeFilter.matches(name("baz").tag("bar", "baz").build(), mockMetric));
	}

	@Test
	public void testInclusiveFilter() {
		MetricNameFilter includeFilter = MetricNameFilter.includePatterns(name("foo").tag("bar", "baz").build());

		assertTrue(includeFilter.matches(name("foo").tag("bar", "baz").build(), mockMetric));
		assertTrue(includeFilter.matches(name("foo").tag("bar", "baz").tag("baz", "bar").build(), mockMetric));
		assertFalse(includeFilter.matches(name("foo").tag("ba", "bar").build(), mockMetric));
		assertFalse(includeFilter.matches(name("baz").tag("bar", "baz").build(), mockMetric));
	}

	@Test
	public void testExclusiveFilter() {
		MetricNameFilter includeFilter = MetricNameFilter.excludePatterns(name("foo").tag("bar", "baz").build());

		assertFalse(includeFilter.matches(name("foo").tag("bar", "baz").build(), mockMetric));
		assertFalse(includeFilter.matches(name("foo").tag("bar", "baz").tag("baz", "bar").build(), mockMetric));
		assertTrue(includeFilter.matches(name("foo").tag("ba", "bar").build(), mockMetric));
		assertTrue(includeFilter.matches(name("baz").tag("bar", "baz").build(), mockMetric));
	}

}
