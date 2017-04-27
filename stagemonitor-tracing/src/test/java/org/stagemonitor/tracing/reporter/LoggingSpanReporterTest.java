package org.stagemonitor.tracing.reporter;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoggingSpanReporterTest {

	private LoggingSpanReporter loggingSpanReporter;
	private TracingPlugin tracingPlugin;

	@Before
	public void setUp() throws Exception {
		tracingPlugin = mock(TracingPlugin.class);
		loggingSpanReporter = new LoggingSpanReporter();
		final ConfigurationRegistry configuration = mock(ConfigurationRegistry.class);
		when(configuration.getConfig(TracingPlugin.class)).thenReturn(tracingPlugin);
		loggingSpanReporter.init(configuration);
	}

	@Test
	public void report() throws Exception {
		final ReadbackSpan readbackSpan = new ReadbackSpan();
		readbackSpan.setTag("foo.bar", "baz");
		SpanContextInformation spanContextInformation = mock(SpanContextInformation.class);
		when(spanContextInformation.getReadbackSpan()).thenReturn(readbackSpan);
		final String logMessage = loggingSpanReporter.getLogMessage(spanContextInformation);
		assertTrue(logMessage.contains("foo.bar: baz"));
	}

	@Test
	public void isActive() throws Exception {
		when(tracingPlugin.isLogSpans()).thenReturn(true);
		assertTrue(loggingSpanReporter.isActive(null));

		when(tracingPlugin.isLogSpans()).thenReturn(false);
		assertFalse(loggingSpanReporter.isActive(null));
	}

	@Test
	public void testLoadedViaServiceLoader() throws Exception {
		List<Class<? extends SpanReporter>> spanReporters = new ArrayList<>();
		ServiceLoader.load(SpanReporter.class).forEach(reporter -> spanReporters.add(reporter.getClass()));
		assertTrue(spanReporters.contains(LoggingSpanReporter.class));
	}

}
