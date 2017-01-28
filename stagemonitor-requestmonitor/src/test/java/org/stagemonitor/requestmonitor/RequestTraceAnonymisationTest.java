package org.stagemonitor.requestmonitor;


import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.requestmonitor.anonymization.AnonymizingSpanInterceptor;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import java.util.Collections;

import io.opentracing.Span;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RequestTraceAnonymisationTest {

	private RequestMonitorPlugin requestMonitorPlugin;

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
	}

	@Test
	public void testAnonymizeUserNameAndIp() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);

		final Span span = createSpan();

		verify(span).setTag("username", "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3");
		verify(span, times(0)).setTag(eq("username_disclosed"), anyString());
		verify(span).setTag(SpanUtils.IPV4_STRING, "123.123.123.0");
	}

	@Test
	public void testAnonymizeIp() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(false);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);

		final Span span = createSpan();

		verify(span).setTag("username", "test");
		verify(span, times(0)).setTag(eq("username_disclosed"), anyString());
		verify(span).setTag(SpanUtils.IPV4_STRING, "123.123.123.0");
	}

	@Test
	public void testDiscloseUserNameAndIp() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);
		when(requestMonitorPlugin.getDiscloseUsers()).thenReturn(Collections.singleton("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3"));

		final Span span = createSpan();

		verify(span).setTag("username", "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3");
		verify(span).setTag("username_disclosed", "test");
		verify(span).setTag(SpanUtils.IPV4_STRING, "123.123.123.123");
	}

	@Test
	public void testDiscloseIp() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(false);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);
		when(requestMonitorPlugin.getDiscloseUsers()).thenReturn(Collections.singleton("test"));

		final Span span = createSpan();


		verify(span).setTag("username", "test");
		verify(span).setTag("username_disclosed", "test");
		verify(span).setTag(SpanUtils.IPV4_STRING, "123.123.123.123");
	}

	@Test
	public void testNull() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);

		final Span span = createSpan(null, null);

		verify(span, atLeastOnce()).setTag("username", (String) null);
		verify(span, times(0)).setTag(eq("username_disclosed"), anyString());
		verify(span).setTag(SpanUtils.IPV4_STRING, (String) null);
	}

	@Test
	public void testNullUser() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);

		final Span span = createSpan(null, "123.123.123.123");

		verify(span, atLeastOnce()).setTag("username", (String) null);
		verify(span, times(0)).setTag(eq("username_disclosed"), anyString());
		verify(span).setTag(SpanUtils.IPV4_STRING, "123.123.123.0");
	}

	@Test
	public void testNullIp() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);

		final Span span = createSpan("test", null);


		verify(span).setTag("username", "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3");
		verify(span, times(0)).setTag(eq("username_disclosed"), anyString());
		verify(span).setTag(SpanUtils.IPV4_STRING, (String) null);
	}

	@Test
	public void testDontAnonymize() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(false);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(false);

		final Span span = createSpan();

		verify(span).setTag("username", "test");
		verify(span, times(0)).setTag(eq("username_disclosed"), anyString());
		verify(span).setTag(SpanUtils.IPV4_STRING, "123.123.123.123");
	}

	private Span createSpan() {
		return createSpan("test", "123.123.123.123");
	}

	private Span createSpan(String username, String ip) {
		final Span span = new SpanWrapper(mock(Span.class), Collections.singletonList(new AnonymizingSpanInterceptor(requestMonitorPlugin)));
		span.setTag(SpanUtils.USERNAME, username);
		SpanUtils.setClientIp(span, ip);
		final Span mockSpan = ((SpanWrapper) span).getDelegate();
		span.finish();
		return mockSpan;
	}

}
