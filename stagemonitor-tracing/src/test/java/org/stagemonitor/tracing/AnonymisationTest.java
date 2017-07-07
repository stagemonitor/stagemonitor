package org.stagemonitor.tracing;


import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.tracing.anonymization.AnonymizingSpanEventListener;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import io.opentracing.Span;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AnonymisationTest {

	private TracingPlugin tracingPlugin;

	@Before
	public void setUp() throws Exception {
		tracingPlugin = mock(TracingPlugin.class);
		final ConfigurationRegistry configuration = mock(ConfigurationRegistry.class);
		final CorePlugin corePlugin = mock(CorePlugin.class);
		when(configuration.getConfig(TracingPlugin.class)).thenReturn(tracingPlugin);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(tracingPlugin.isAnonymizeIPs()).thenReturn(true);
		when(tracingPlugin.getDiscloseUsers()).thenReturn(Collections.emptySet());
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
	}

	@Test
	public void testAnonymizeUserNameAndIp() throws Exception {
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(tracingPlugin.isAnonymizeIPs()).thenReturn(true);

		final Span span = createSpan();

		verify(span).setTag("username", "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3");
		verify(span, times(0)).setTag(eq("username_disclosed"), anyString());
		verify(span).setTag(SpanUtils.IPV4_STRING, "123.123.123.0");
	}

	@Test
	public void testAnonymizeIp() throws Exception {
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(false);
		when(tracingPlugin.isAnonymizeIPs()).thenReturn(true);

		final Span span = createSpan();

		verify(span).setTag("username", "test");
		verify(span, times(0)).setTag(eq("username_disclosed"), anyString());
		verify(span).setTag(SpanUtils.IPV4_STRING, "123.123.123.0");
	}

	@Test
	public void testDiscloseUserNameAndIp() throws Exception {
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(tracingPlugin.isAnonymizeIPs()).thenReturn(true);
		when(tracingPlugin.getDiscloseUsers()).thenReturn(Collections.singleton("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3"));

		final Span span = createSpan();

		verify(span).setTag("username", "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3");
		verify(span).setTag("username_disclosed", "test");
		verify(span).setTag(SpanUtils.IPV4_STRING, "123.123.123.123");
	}

	@Test
	public void testDiscloseIp() throws Exception {
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(false);
		when(tracingPlugin.isAnonymizeIPs()).thenReturn(true);
		when(tracingPlugin.getDiscloseUsers()).thenReturn(Collections.singleton("test"));

		final Span span = createSpan();


		verify(span).setTag("username", "test");
		verify(span).setTag("username_disclosed", "test");
		verify(span).setTag(SpanUtils.IPV4_STRING, "123.123.123.123");
	}

	@Test
	public void testNull() throws Exception {
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(tracingPlugin.isAnonymizeIPs()).thenReturn(true);

		final Span span = createSpan(null, null);

		verify(span, atLeastOnce()).setTag("username", (String) null);
		verify(span, times(0)).setTag(eq("username_disclosed"), anyString());
		verify(span, times(0)).setTag(eq(SpanUtils.IPV4_STRING), anyString());
	}

	@Test
	public void testNullUser() throws Exception {
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(tracingPlugin.isAnonymizeIPs()).thenReturn(true);

		final Span span = createSpan(null, "123.123.123.123");

		verify(span, atLeastOnce()).setTag("username", (String) null);
		verify(span, times(0)).setTag(eq("username_disclosed"), anyString());
		verify(span).setTag(SpanUtils.IPV4_STRING, "123.123.123.0");
	}

	@Test
	public void testNullIp() throws Exception {
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(tracingPlugin.isAnonymizeIPs()).thenReturn(true);

		final Span span = createSpan("test", null);


		verify(span).setTag("username", "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3");
		verify(span, times(0)).setTag(eq("username_disclosed"), anyString());
		verify(span, times(0)).setTag(eq(SpanUtils.IPV4_STRING), anyString());
	}

	@Test
	public void testDontAnonymize() throws Exception {
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(false);
		when(tracingPlugin.isAnonymizeIPs()).thenReturn(false);

		final Span span = createSpan();

		verify(span).setTag("username", "test");
		verify(span, times(0)).setTag(eq("username_disclosed"), anyString());
		verify(span).setTag(SpanUtils.IPV4_STRING, "123.123.123.123");
	}

	private Span createSpan() {
		return createSpan("test", "123.123.123.123");
	}

	private Span createSpan(String username, String ip) {
		final Span span = new SpanWrapper(spy(NoopSpan.INSTANCE), "", 0, 0, Collections.singletonList(new AnonymizingSpanEventListener(tracingPlugin)), new ConcurrentHashMap<>());
		span.setTag(SpanUtils.USERNAME, username);
		SpanUtils.setClientIp(span, ip);
		final Span mockSpan = ((SpanWrapper) span).getDelegate();
		span.finish();
		return mockSpan;
	}

}
