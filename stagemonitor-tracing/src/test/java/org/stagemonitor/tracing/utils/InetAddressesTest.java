package org.stagemonitor.tracing.utils;

import org.junit.Test;
import org.stagemonitor.core.util.InetAddresses;

import java.net.Inet4Address;
import java.net.InetAddress;

import static org.assertj.core.api.Assertions.assertThat;

public class InetAddressesTest {

	@Test
	public void setClientIp() {
		final InetAddress inetAddress = InetAddresses.forString("127.0.0.1");
		final int ipAsInt = InetAddresses.inetAddressToInt((Inet4Address) inetAddress);
		final String ip = InetAddresses.fromInteger(ipAsInt).getHostAddress();
		assertThat(ip).isEqualTo("127.0.0.1");
	}
}
