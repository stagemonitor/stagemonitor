package org.stagemonitor.tracing.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class IPAnonymizationUtils {

	private static final Logger logger = LoggerFactory.getLogger(IPAnonymizationUtils.class);

	private static final int ANONYMIZED_IPV4_OCTETS = 1;
	private static final int ANONYMIZED_IPV6_OCTETS = 10;

	private IPAnonymizationUtils() {
		// don't instantiate
	}

	/**
	 * Anonymizes IPv4 and IPv6 addresses
	 * <p/>
	 * For IPv4 addresses, the last octet is set to zero.
	 * If the address is a IPv6 address, the last 80 bits (10 bytes) are set to zero.
	 * <p/>
	 * This is just like Google Analytics handles IP anonymization: https://support.google.com/analytics/answer/2763052
	 *
	 * @param clientIp the full IPv4 or IPv6 address
	 * @return the anonymized IP address or null, if the provided ip address is invalid
	 */
	public static String anonymize(String clientIp) {
		// prevents DNS lookups
		if (clientIp == null || (!InetAddressUtils.isIPv4Address(clientIp) && !InetAddressUtils.isIPv6Address(clientIp))) {
			return null;
		}
		try {
			// if clientIp is not a ip address, that would perform a DNS lookup
			final InetAddress inetAddress = InetAddress.getByName(clientIp);
			if (inetAddress instanceof Inet4Address) {
				final Inet4Address inet4Address = (Inet4Address) inetAddress;
				final byte[] address = inet4Address.getAddress();
				anonymizeLastBytes(address, ANONYMIZED_IPV4_OCTETS);
				return InetAddress.getByAddress(address).toString().substring(1);
			} else if (inetAddress instanceof Inet6Address) {
				final Inet6Address inet6Address = (Inet6Address) inetAddress;
				final byte[] address = inet6Address.getAddress();
				anonymizeLastBytes(address, ANONYMIZED_IPV6_OCTETS);
				return InetAddress.getByAddress(address).toString().substring(1);
			}
		} catch (UnknownHostException e) {
			logger.warn(e.getMessage(), e);
		}
		return null;
	}

	private static void anonymizeLastBytes(final byte[] address, final int bytesToAnonymize) {
		for (int i = bytesToAnonymize; i > 0; i--) {
			address[address.length - i] = 0;
		}
	}

}
