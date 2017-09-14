package org.stagemonitor.tracing.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.util.InetAddresses;

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
	 * <p>
	 * For IPv4 addresses, the last octet is set to zero. If the address is a IPv6 address, the last 80 bits (10 bytes)
	 * are set to zero.
	 * <p>
	 * This is just like Google Analytics handles IP anonymization: https://support.google.com/analytics/answer/2763052
	 *
	 * @param ip the full IPv4 or IPv6 address
	 * @return the anonymized IP address or null, if the provided ip address is invalid
	 */
	public static String anonymize(String ip) {
		InetAddress inetAddress;
		try {
			inetAddress = InetAddresses.forString(ip);
		} catch (IllegalArgumentException e) {
			logger.warn(e.getMessage(), e);
			inetAddress = null;
		}
		InetAddress anonymized = null;
		if (inetAddress instanceof Inet4Address) {
			anonymized = anonymizeIpV4Address((Inet4Address) inetAddress);
		} else if (inetAddress instanceof Inet6Address) {
			anonymized = anonymizeIpV6Address((Inet6Address) inetAddress);
		}
		if (anonymized != null) {
			return anonymized.getHostAddress();
		} else {
			return null;
		}
	}

	/**
	 * Anonymizes IPv4 addresses
	 * <p>
	 * The last octet is set to zero.
	 * <p>
	 * This is just like Google Analytics handles IP anonymization: https://support.google.com/analytics/answer/2763052
	 *
	 * @param inetAddress the full IPv4 address
	 * @return the anonymized IP address
	 */
	public static Inet4Address anonymizeIpV4Address(Inet4Address inetAddress) {
		try {
			final byte[] address = inetAddress.getAddress();
			anonymizeLastBytes(address, ANONYMIZED_IPV4_OCTETS);
			return (Inet4Address) InetAddress.getByAddress(address);
		} catch (UnknownHostException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Anonymizes IPv6 addresses
	 * <p>
	 * The last 80 bits (10 bytes) are set to zero.
	 * <p>
	 * This is just like Google Analytics handles IP anonymization: https://support.google.com/analytics/answer/2763052
	 *
	 * @param inet6Address the full IPv6 address
	 * @return the anonymized IP address or null, if the provided ip address is invalid
	 */
	public static Inet6Address anonymizeIpV6Address(Inet6Address inet6Address) {
		final byte[] address = inet6Address.getAddress();
		anonymizeLastBytes(address, ANONYMIZED_IPV6_OCTETS);
		try {
			return (Inet6Address) InetAddress.getByAddress(address);
		} catch (UnknownHostException e) {
			throw new AssertionError(e);
		}
	}

	private static void anonymizeLastBytes(final byte[] address, final int bytesToAnonymize) {
		for (int i = bytesToAnonymize; i > 0; i--) {
			address[address.length - i] = 0;
		}
	}

}
