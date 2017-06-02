package org.stagemonitor.tracing.tracing.jaeger;

import com.codahale.metrics.SharedMetricRegistries;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.mdc.MDCSpanEventListener;
import org.stagemonitor.tracing.tracing.B3Propagator;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import java.util.Collections;

import io.opentracing.mock.MockTracer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MDCSpanEventListenerTest {

	private MDCSpanEventListener mdcSpanInterceptor;
	private CorePlugin corePlugin;
	private SpanWrapper spanWrapper;

	@Before
	public void setUp() throws Exception {
		Stagemonitor.reset();
		SharedMetricRegistries.clear();
		this.corePlugin = mock(CorePlugin.class);
		when(corePlugin.isStagemonitorActive()).thenReturn(true);

		final MockTracer tracer = new MockTracer(new B3Propagator());

		TracingPlugin tracingPlugin = mock(TracingPlugin.class);
		when(tracingPlugin.getTracer()).thenReturn(tracer);

		mdcSpanInterceptor = new MDCSpanEventListener(corePlugin, tracingPlugin);
		spanWrapper = new SpanWrapper(tracer.buildSpan("operation name").start(),"operation name",
				1, 1, Collections.emptyList());
	}

	@After
	public void tearDown() throws Exception {
		Stagemonitor.reset();
		MDC.clear();
	}

	@Test
	public void testMdc() throws Exception {
		Stagemonitor.startMonitoring(new MeasurementSession("MDCSpanEventListenerTest", "testHost", "testInstance"));
		when(corePlugin.getMeasurementSession())
				.thenReturn(new MeasurementSession("MDCSpanEventListenerTest", "testHost", "testInstance"));
		mdcSpanInterceptor.onStart(spanWrapper);

		assertNotNull(MDC.get("spanId"));
		assertNotNull(MDC.get("traceId"));
		assertNull(MDC.get("parentId"));

		mdcSpanInterceptor.onFinish(spanWrapper, null, 0);
		assertEquals("testHost", MDC.get("host"));
		assertEquals("MDCSpanEventListenerTest", MDC.get("application"));
		assertEquals("testInstance", MDC.get("instance"));
		assertNull(MDC.get("spanId"));
		assertNull(MDC.get("traceId"));
		assertNull(MDC.get("parentId"));
	}

	@Test
	public void testMdcStagemonitorNotStarted() throws Exception {
		when(corePlugin.getMeasurementSession())
				.thenReturn(new MeasurementSession("MDCSpanEventListenerTest", "testHost", null));

		mdcSpanInterceptor.onStart(spanWrapper);
		assertEquals("testHost", MDC.get("host"));
		assertEquals("MDCSpanEventListenerTest", MDC.get("application"));
		assertNull(MDC.get("instance"));
		assertNull(MDC.get("spanId"));
		assertNull(MDC.get("traceId"));
		assertNull(MDC.get("parentId"));
		mdcSpanInterceptor.onFinish(spanWrapper, null, 0);
	}

	@Test
	public void testMDCStagemonitorDeactivated() throws Exception {
		when(corePlugin.isStagemonitorActive()).thenReturn(false);
		when(corePlugin.getMeasurementSession())
				.thenReturn(new MeasurementSession("MDCSpanEventListenerTest", "testHost", null));

		mdcSpanInterceptor.onStart(spanWrapper);

		assertNull(MDC.getCopyOfContextMap());
	}

}
