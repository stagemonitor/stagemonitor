package org.stagemonitor;

import org.junit.Test;
import org.mockito.Mockito;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.web.servlet.filter.AbstractExclusionFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractExclusionFilterTest {

	private AbstractExclusionFilter testFilter;
	private HttpServletRequest mockRequest = mock(HttpServletRequest.class);

	private static class TestExclusionFilter extends AbstractExclusionFilter {

		TestExclusionFilter(ConfigurationOption<Collection<String>> excludedPaths, ConfigurationOption<Collection<String>> excludedPathsAntPattern) {
			super(excludedPaths, excludedPathsAntPattern);
		}

		@Override
		public void doFilterInternal(HttpServletRequest servletRequest, HttpServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		}
	}

	@Test
	public void testExclude() throws Exception {
		testFilter = Mockito.spy(new TestExclusionFilter(
				ConfigurationOption.stringsOption().buildWithDefault(Arrays.asList("/exclude1", "/exclude2", "/exclude3/")),
				ConfigurationOption.stringsOption().buildWithDefault(Collections.singletonList("/foo/**"))
		));
		assertExcludes("/exclude1");
		assertExcludes("/exclude2/bla/blubb");
		assertExcludes("/exclude3/");
		assertExcludes("/exclude2bla");

		assertIncludes("/exclude3");
		assertIncludes("/included");
		assertIncludes("/included/exclude1");
	}

	@Test
	public void testExcludeAntPath() throws Exception {
		testFilter = Mockito.spy(new TestExclusionFilter(
				ConfigurationOption.stringsOption().buildWithDefault(Collections.singletonList("/foo")),
				ConfigurationOption.stringsOption().buildWithDefault(Arrays.asList("/**/*.js", "/exclude/**"))
		));
		assertExcludes("/bar.js");
		assertExcludes("/exclude/bla/blubb");
		assertExcludes("/exclude");
	}

	@Test
	public void testNotExclude() throws Exception {
		testFilter = Mockito.spy(new TestExclusionFilter(
				ConfigurationOption.stringsOption().buildWithDefault(Collections.emptyList()),
				ConfigurationOption.stringsOption().buildWithDefault(Collections.emptyList())
		));
		assertIncludes("/exclude3");
	}

	private void assertIncludes(String url) throws Exception {
		assertExcludes(url, false);
	}

	private void assertExcludes(String url) throws Exception {
		assertExcludes(url, true);
	}

	private int notExclutedCount = 0;

	private void assertExcludes(String url, boolean excluded) throws Exception {
		if (!excluded)
			notExclutedCount++;

		when(mockRequest.getRequestURI()).thenReturn(url);
		when(mockRequest.getServletPath()).thenReturn(url);
		when(mockRequest.getContextPath()).thenReturn("/context-path");
		testFilter.doFilter(mockRequest, mock(HttpServletResponse.class), mock(FilterChain.class));
		verify(testFilter, times(notExclutedCount)).doFilterInternal(any(), any(), any());
	}
}
