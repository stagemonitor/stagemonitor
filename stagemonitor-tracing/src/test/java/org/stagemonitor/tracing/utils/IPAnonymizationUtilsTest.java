package org.stagemonitor.tracing.utils;

import org.junit.Assert;
import org.junit.Test;

public class IPAnonymizationUtilsTest {

	@Test
	public void testAnonymizeIpv4() throws Exception {
		Assert.assertEquals("192.123.123.0", IPAnonymizationUtils.anonymize("192.123.123.123"));
	}

	@Test
	public void testAnonymizeIpv6Full() throws Exception {
		Assert.assertEquals("fe80:fe1e:b3ff:0:0:0:0:0", IPAnonymizationUtils.anonymize("FE80:FE1E:B3FF:0000:0202:B3FF:FE1E:8329"));
	}

	@Test
	public void testAnonymizeIpv6Collapsed() throws Exception {
		Assert.assertEquals("fe80:202:0:0:0:0:0:0", IPAnonymizationUtils.anonymize("FE80:0202::B3FF:FE1E:8329"));
	}

	@Test
	public void testAnonymizeInvalid() throws Exception {
		Assert.assertNull(IPAnonymizationUtils.anonymize("java.sun.com"));
		Assert.assertNull(IPAnonymizationUtils.anonymize("foo.java.sun.com"));
		Assert.assertNull(IPAnonymizationUtils.anonymize("foo:bar"));
		Assert.assertNull(IPAnonymizationUtils.anonymize("foo:bar:baz:foo:bar:baz:foo:bar"));
		Assert.assertNull(IPAnonymizationUtils.anonymize("foo.bar"));
		Assert.assertNull(IPAnonymizationUtils.anonymize("foobar"));
	}
}
