package org.stagemonitor.tracing.anonymization;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.util.InetAddresses;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnonymizingSpanEventListenerTest {

	private static final Inet4Address IPV4 = (Inet4Address) InetAddresses.forString("123.123.123.123");
	private static final Inet4Address IPV4_ANONYMIZED = (Inet4Address) InetAddresses.forString("123.123.123.0");
	private static final Inet6Address IPV6 = (Inet6Address) InetAddresses.forString("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
	private static final Inet6Address IPV6_ANONYMIZED = (Inet6Address) InetAddresses.forString("2001:0db8:85a3:0000:0000:0000:0000:0000");
	private TracingPlugin tracingPlugin;

	private MockTracer mockTracer = new MockTracer();

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
		assertThat(mockTracer.scopeManager().activeSpan()).isNull();
	}

	@After
	public void tearDown() throws Exception {
		assertThat(mockTracer.scopeManager().activeSpan()).isNull();
	}

	@Test
	public void testAnonymizeUserNameAndIpV4() throws Exception {
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(tracingPlugin.isAnonymizeIPs()).thenReturn(true);

		final SpanWrapper span = createSpanIpv4();

		assertThat(span.getTags()).containsEntry("username", "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3");
		assertThat(span.getTags()).doesNotContainKey("username_disclosed");
		assertThat(span.getTags()).containsEntry(Tags.PEER_HOST_IPV4.getKey(), InetAddresses.inetAddressToInt(IPV4_ANONYMIZED));
	}

	@Test
	public void testAnonymizeIpv6() throws Exception {
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(false);
		when(tracingPlugin.isAnonymizeIPs()).thenReturn(true);

		final SpanWrapper span = createSpan("test", IPV6.getHostAddress());

		assertThat(span.getTags()).containsEntry("username", "test");
		assertThat(span.getTags()).doesNotContainKey("username_disclosed");
		assertThat(span.getTags()).containsEntry(Tags.PEER_HOST_IPV6.getKey(), IPV6_ANONYMIZED.getHostAddress());
	}

	@Test
	public void testAnonymizeIpV4() throws Exception {
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(false);
		when(tracingPlugin.isAnonymizeIPs()).thenReturn(true);

		final SpanWrapper span = createSpanIpv4();

		assertThat(span.getTags()).containsEntry("username", "test");
		assertThat(span.getTags()).doesNotContainKey("username_disclosed");
		assertThat(span.getTags()).containsEntry(Tags.PEER_HOST_IPV4.getKey(), InetAddresses.inetAddressToInt(IPV4_ANONYMIZED));
	}

	@Test
	public void testDiscloseUserNameAndIp() throws Exception {
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(tracingPlugin.isAnonymizeIPs()).thenReturn(true);
		when(tracingPlugin.getDiscloseUsers()).thenReturn(Collections.singleton("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3"));

		final SpanWrapper span = createSpanIpv4();

		assertThat(span.getTags()).containsEntry("username", "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3");
		assertThat(span.getTags()).containsEntry("username_disclosed", "test");
		assertThat(span.getTags()).containsEntry(Tags.PEER_HOST_IPV4.getKey(), InetAddresses.inetAddressToInt(IPV4));
	}

	@Test
	public void testDiscloseIp() throws Exception {
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(false);
		when(tracingPlugin.isAnonymizeIPs()).thenReturn(true);
		when(tracingPlugin.getDiscloseUsers()).thenReturn(Collections.singleton("test"));

		final SpanWrapper span = createSpanIpv4();


		assertThat(span.getTags()).containsEntry("username", "test");
		assertThat(span.getTags()).containsEntry("username_disclosed", "test");
		assertThat(span.getTags()).containsEntry(Tags.PEER_HOST_IPV4.getKey(), InetAddresses.inetAddressToInt(IPV4));
	}

	@Test
	public void testNull() throws Exception {
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(tracingPlugin.isAnonymizeIPs()).thenReturn(true);

		final SpanWrapper span = createSpan(null, null);

		assertThat(span.getTags()).doesNotContainKey("username");
		assertThat(span.getTags()).doesNotContainKey("username_disclosed");
		assertThat(span.getTags()).doesNotContainKey(SpanUtils.IPV4_STRING);
	}

	@Test
	public void testNullUser() throws Exception {
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(tracingPlugin.isAnonymizeIPs()).thenReturn(true);

		final SpanWrapper span = createSpan(null, IPV4.getHostAddress());

		assertThat(span.getTags()).doesNotContainKey("username");
		assertThat(span.getTags()).doesNotContainKey("username_disclosed");
		assertThat(span.getTags()).containsEntry(Tags.PEER_HOST_IPV4.getKey(), InetAddresses.inetAddressToInt(IPV4_ANONYMIZED));
	}

	@Test
	public void testNullIp() throws Exception {
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(tracingPlugin.isAnonymizeIPs()).thenReturn(true);

		final SpanWrapper span = createSpan("test", null);

		assertThat(span.getTags()).containsEntry("username", "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3");
		assertThat(span.getTags()).doesNotContainKey("username_disclosed");
		assertThat(span.getTags()).doesNotContainKey(SpanUtils.IPV4_STRING);
	}

	@Test
	public void testDontAnonymize() throws Exception {
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(false);
		when(tracingPlugin.isAnonymizeIPs()).thenReturn(false);

		final SpanWrapper span = createSpanIpv4();

		assertThat(span.getTags()).containsEntry("username", "test");
		assertThat(span.getTags()).doesNotContainKey("username_disclosed");
		assertThat(span.getTags()).containsEntry(Tags.PEER_HOST_IPV4.getKey(), InetAddresses.inetAddressToInt(IPV4));
	}

	private SpanWrapper createSpanIpv4() {
		return createSpan("test", IPV4.getHostAddress());
	}

	private SpanWrapper createSpan(String username, String ip) {
		final SpanWrapper span = new SpanWrapper(
				mockTracer.buildSpan("").start(),
				"", 0, 0,
				Collections.singletonList(new AnonymizingSpanEventListener(tracingPlugin)), new ConcurrentHashMap<>());
		span.setTag(SpanUtils.USERNAME, username);
		SpanUtils.setClientIp(span, ip);
		span.finish();
		return span;
	}

}
