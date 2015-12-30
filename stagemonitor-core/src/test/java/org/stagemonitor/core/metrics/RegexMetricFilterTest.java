package org.stagemonitor.core.metrics;

import java.util.Collections;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mockito.Mockito;

import com.codahale.metrics.Metric;

public class RegexMetricFilterTest {
	
	private final Metric mockMetric = Mockito.mock(Metric.class);
	
	@Test
	public void testInclusiveFilter() {
		RegexMetricFilter includeFilter = 
				RegexMetricFilter.includePatterns(
						Collections.singleton(Pattern.compile(".*foo.*")));
		
		assertTrue(includeFilter.matches("aaaafooobbb", mockMetric));
		assertTrue(includeFilter.matches("fooobbb", mockMetric));
		assertTrue(includeFilter.matches("aaaafooo", mockMetric));
		assertFalse(includeFilter.matches("barbarbar", mockMetric));
	}
	
	@Test
	public void testExclusiveFilter() {
		RegexMetricFilter excludeFilter = 
				RegexMetricFilter.excludePatterns(
						Collections.singleton(Pattern.compile(".*foo.*")));
		
		assertFalse(excludeFilter.matches("aaaafooobbb", mockMetric));
		assertFalse(excludeFilter.matches("fooobbb", mockMetric));
		assertFalse(excludeFilter.matches("aaaafooo", mockMetric));
		assertTrue(excludeFilter.matches("barbarbar", mockMetric));
	}

}
