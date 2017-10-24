package org.stagemonitor.tracing.reporter;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.tracing.B3Propagator;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalScopeManager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoggingSpanReporterTest {

	private LoggingSpanReporter loggingSpanReporter;
	private TracingPlugin tracingPlugin;
	private MockTracer mockTracer;

	@Before
	public void setUp() throws Exception {
		tracingPlugin = mock(TracingPlugin.class);
		mockTracer = new MockTracer(new ThreadLocalScopeManager(), new B3Propagator());
		when(tracingPlugin.getTracer()).thenReturn(mockTracer);
		loggingSpanReporter = new LoggingSpanReporter();
		final ConfigurationRegistry configuration = mock(ConfigurationRegistry.class);
		when(configuration.getConfig(TracingPlugin.class)).thenReturn(tracingPlugin);
		loggingSpanReporter.init(configuration);
	}

	@Test
	public void report() throws Exception {
		final SpanWrapper span = new SpanWrapper(mockTracer.buildSpan("test").start(), "test", 0, 0, Collections.emptyList(), new ConcurrentHashMap<>());
		span.setTag("foo.bar", "baz");
		final String logMessage = loggingSpanReporter.getLogMessage(span);
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
