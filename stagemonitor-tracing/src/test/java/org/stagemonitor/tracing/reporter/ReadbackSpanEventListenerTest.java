package org.stagemonitor.tracing.reporter;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.tracing.B3Propagator;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import java.util.Collections;

import io.opentracing.mock.MockTracer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReadbackSpanEventListenerTest {

	private TracingPlugin tracingPlugin;
	private ReportingSpanEventListener reportingSpanEventListener;
	private ReadbackSpanEventListener readbackSpanEventListener;
	private SpanWrapper spanWrapper;

	@Before
	public void setUp() throws Exception {
		tracingPlugin = mock(TracingPlugin.class);
		reportingSpanEventListener = mock(ReportingSpanEventListener.class);
		readbackSpanEventListener = new ReadbackSpanEventListener.Factory(reportingSpanEventListener, tracingPlugin).create();

		final MockTracer tracer = new MockTracer(new B3Propagator());

		when(tracingPlugin.getTracer()).thenReturn(tracer);
		spanWrapper = new SpanWrapper(tracer.buildSpan("operation name").start(),"operation name",
				1, 1, Collections.emptyList());
	}

	@Test
	public void testReadback() throws Exception {
		when(reportingSpanEventListener.isAnyReporterActive(any())).thenReturn(true);
		readbackSpanEventListener.onStart(spanWrapper);
		readbackSpanEventListener.onSetTag("string", "foo");
		readbackSpanEventListener.onSetTag("boolean", true);
		readbackSpanEventListener.onSetTag("number", 42);
		readbackSpanEventListener.onFinish(spanWrapper, "operation name", 1);
		final ReadbackSpan readbackSpan = readbackSpanEventListener.getReadbackSpan();
		assertNotNull(readbackSpan);
		assertNotNull(readbackSpan.getId());
		assertNotNull(readbackSpan.getTraceId());
		assertEquals("foo", readbackSpan.getTags().get("string"));
		assertEquals(true, readbackSpan.getTags().get("boolean"));
		assertEquals(42, readbackSpan.getTags().get("number"));
		assertEquals("operation name", readbackSpan.getName());
	}

	@Test
	public void testNoReporterActive() throws Exception {
		when(reportingSpanEventListener.isAnyReporterActive(any())).thenReturn(false);
		final SpanWrapper spanWrapper = mock(SpanWrapper.class);
		readbackSpanEventListener.onStart(spanWrapper);
		readbackSpanEventListener.onFinish(spanWrapper, "op", 1);
		assertNull(readbackSpanEventListener.getReadbackSpan());
	}
}
