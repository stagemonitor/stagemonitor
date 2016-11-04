package org.stagemonitor.requestmonitor;

import com.uber.jaeger.Span;
import com.uber.jaeger.Tracer;
import com.uber.jaeger.reporters.NoopReporter;
import com.uber.jaeger.samplers.ConstSampler;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.utils.SpanTags;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestTraceAnonymisationTest {

	private RequestMonitorPlugin requestMonitorPlugin;
	private RequestMonitor requestMonitor;

	@Before
	public void setUp() throws Exception {
		requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		final Configuration configuration = mock(Configuration.class);
		final CorePlugin corePlugin = mock(CorePlugin.class);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);
		when(requestMonitorPlugin.getDiscloseUsers()).thenReturn(Collections.emptySet());
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		requestMonitor = new RequestMonitor(configuration, mock(Metric2Registry.class), Collections.emptyList());
	}

	@Test
	public void testAnonymizeUserNameAndIp() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);

		final Span span = createSpan();

		assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", span.getTags().get("username"));
		assertNull(span.getTags().get("username_disclosed"));
		assertEquals("123.123.123.0", span.getTags().get(SpanTags.IPV4_STRING));
	}

	@Test
	public void testAnonymizeIp() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(false);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);

		final Span span = createSpan();

		assertEquals("test", span.getTags().get("username"));
		assertNull(span.getTags().get("username_disclosed"));
		assertEquals("123.123.123.0", span.getTags().get(SpanTags.IPV4_STRING));
	}

	@Test
	public void testDiscloseUserNameAndIp() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);
		when(requestMonitorPlugin.getDiscloseUsers()).thenReturn(Collections.singleton("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3"));

		final Span span = createSpan();

		assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", span.getTags().get("username"));
		assertEquals("test", span.getTags().get("username_disclosed"));
		assertEquals("123.123.123.123", span.getTags().get(SpanTags.IPV4_STRING));
	}

	@Test
	public void testDiscloseIp() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(false);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);
		when(requestMonitorPlugin.getDiscloseUsers()).thenReturn(Collections.singleton("test"));

		final Span span = createSpan();

		assertEquals("test", span.getTags().get("username"));
		assertEquals("test", span.getTags().get("username_disclosed"));
		assertEquals("123.123.123.123", span.getTags().get(SpanTags.IPV4_STRING));
	}

	@Test
	public void testNull() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);

		final Span span = createSpan(null, null);

		assertNull(span.getTags().get("username"));
		assertNull(span.getTags().get("username_disclosed"));
		assertNull(span.getTags().get(SpanTags.IPV4_STRING));
	}

	@Test
	public void testNullUser() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);

		final Span span = createSpan(null, "123.123.123.123");

		assertNull(span.getTags().get("username"));
		assertNull(span.getTags().get("username_disclosed"));
		assertEquals("123.123.123.0", span.getTags().get(SpanTags.IPV4_STRING));
	}

	@Test
	public void testNullIp() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);

		final Span span = createSpan("test", null);

		assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", span.getTags().get("username"));
		assertNull(span.getTags().get("username_disclosed"));
		assertNull(span.getTags().get(SpanTags.IPV4_STRING));
	}

	@Test
	public void testDontAnonymize() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(false);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(false);

		final Span span = createSpan();

		assertEquals("test", span.getTags().get("username"));
		assertNull(span.getTags().get("username_disclosed"));
		assertEquals("123.123.123.123", span.getTags().get(SpanTags.IPV4_STRING));
	}

	private Span createSpan() {
		return createSpan("test", "123.123.123.123");
	}

	private Span createSpan(String username, String ip) {
		final Span span = (Span) new Tracer.Builder(getClass().getSimpleName(), new NoopReporter(), new ConstSampler(true))
				.build()
				.buildSpan("Test Operation")
				.start();
		span.setTag(SpanTags.USERNAME, username);
		SpanTags.setClientIp(span, ip);
		requestMonitor.anonymizeUserNameAndIp(span);
		return span;
	}

}
