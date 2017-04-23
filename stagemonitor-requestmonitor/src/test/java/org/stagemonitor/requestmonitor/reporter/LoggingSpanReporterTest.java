package org.stagemonitor.requestmonitor.reporter;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.SpanContextInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoggingSpanReporterTest {

	private LoggingSpanReporter loggingSpanReporter;
	private RequestMonitorPlugin requestMonitorPlugin;

	@Before
	public void setUp() throws Exception {
		requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		loggingSpanReporter = new LoggingSpanReporter();
		final Configuration configuration = mock(Configuration.class);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		loggingSpanReporter.init(configuration);
	}

	@Test
	public void report() throws Exception {
		final ReadbackSpan readbackSpan = new ReadbackSpan();
		readbackSpan.setTag("foo.bar", "baz");
		final SpanContextInformation context = SpanContextInformation.forUnitTest(readbackSpan);
		final String logMessage = loggingSpanReporter.getLogMessage(context);
		assertTrue(logMessage.contains("foo.bar: baz"));
	}

	@Test
	public void isActive() throws Exception {
		when(requestMonitorPlugin.isLogSpans()).thenReturn(true);
		assertTrue(loggingSpanReporter.isActive(null));

		when(requestMonitorPlugin.isLogSpans()).thenReturn(false);
		assertFalse(loggingSpanReporter.isActive(null));
	}

	@Test
	public void testLoadedViaServiceLoader() throws Exception {
		List<Class<? extends SpanReporter>> spanReporters = new ArrayList<>();
		ServiceLoader.load(SpanReporter.class).forEach(reporter -> spanReporters.add(reporter.getClass()));
		assertTrue(spanReporters.contains(LoggingSpanReporter.class));
	}

}
