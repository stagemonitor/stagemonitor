/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.stagemonitor.core.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Locale;

import static org.stagemonitor.core.util.Assert.checkArgument;

/**
 * Copy of {@code com.google.common.net.InetAddresses}.
 *
 * Static utility methods pertaining to {@link InetAddress} instances.
 *
 * <p><b>Important note:</b> Unlike {@code InetAddress.getByName()}, the methods of this class never
 * cause DNS services to be accessed. For this reason, you should prefer these methods as much as
 * possible over their JDK equivalents whenever you are expecting to handle only IP address string
 * literals -- there is no blocking DNS penalty for a malformed string.
 *
 * <p>When dealing with {@link Inet4Address} and {@link Inet6Address} objects as byte arrays (vis.
 * {@code InetAddress.getAddress()}) they are 4 and 16 bytes in length, respectively, and represent
 * the address in network byte order.
 *
 * <p>Examples of IP addresses and their byte representations:
 *
 * <dl>
 * <dt>The IPv4 loopback address, {@code "127.0.0.1"}.
 * <dd>{@code 7f 00 00 01}
 *
 * <dt>The IPv6 loopback address, {@code "::1"}.
 * <dd>{@code 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01}
 *
 * <dt>From the IPv6 reserved documentation prefix ({@code 2001:db8::/32}), {@code "2001:db8::1"}.
 * <dd>{@code 20 01 0d b8 00 00 00 00 00 00 00 00 00 00 00 01}
 *
 * <dt>An IPv6 "IPv4 compatible" (or "compat") address, {@code "::192.168.0.1"}.
 * <dd>{@code 00 00 00 00 00 00 00 00 00 00 00 00 c0 a8 00 01}
 *
 * <dt>An IPv6 "IPv4 mapped" address, {@code "::ffff:192.168.0.1"}.
 * <dd>{@code 00 00 00 00 00 00 00 00 00 00 ff ff c0 a8 00 01}
 *
 * </dl>
 *
 * <p>A few notes about IPv6 "IPv4 mapped" addresses and their observed use in Java.
 *
 * <p>"IPv4 mapped" addresses were originally a representation of IPv4 addresses for use on an IPv6
 * socket that could receive both IPv4 and IPv6 connections (by disabling the {@code IPV6_V6ONLY}
 * socket option on an IPv6 socket). Yes, it's confusing. Nevertheless, these "mapped" addresses
 * were never supposed to be seen on the wire. That assumption was dropped, some say mistakenly, in
 * later RFCs with the apparent aim of making IPv4-to-IPv6 transition simpler.
 *
 * <p>Technically one <i>can</i> create a 128bit IPv6 address with the wire format of a "mapped"
 * address, as shown above, and transmit it in an IPv6 packet header. However, Java's InetAddress
 * creation methods appear to adhere doggedly to the original intent of the "mapped" address: all
 * "mapped" addresses return {@link Inet4Address} objects.
 *
 * <p>For added safety, it is common for IPv6 network operators to filter all packets where either
 * the source or destination address appears to be a "compat" or "mapped" address. Filtering
 * suggestions usually recommend discarding any packets with source or destination addresses in the
 * invalid range {@code ::/3}, which includes both of these bizarre address formats. For more
 * information on "bogons", including lists of IPv6 bogon space, see:
 *
 * <ul>
 * <li><a target="_parent" href="http://en.wikipedia.org/wiki/Bogon_filtering">http://en.wikipedia.
 * org/wiki/Bogon_filtering</a>
 * <li><a target="_parent" href="http://www.cymru.com/Bogons/ipv6.txt">http://www.cymru.com/Bogons/
 * ipv6.txt</a>
 * <li><a target="_parent" href="http://www.cymru.com/Bogons/v6bogon.html">http://www.cymru.com/
 * Bogons/v6bogon.html</a>
 * <li><a target="_parent" href="http://www.space.net/~gert/RIPE/ipv6-filters.html">http://www.
 * space.net/~gert/RIPE/ipv6-filters.html</a>
 * </ul>
 *
 * @author Erik Kline
 * @since 5.0
 */
public final class InetAddresses {
	private static final int IPV4_PART_COUNT = 4;
	private static final int IPV6_PART_COUNT = 8;

	private InetAddresses() {
	}

	/**
	 * Returns an {@link Inet4Address}, given a byte array representation of the IPv4 address.
	 *
	 * @param bytes byte array representing an IPv4 address (should be of length 4)
	 * @return {@link Inet4Address} corresponding to the supplied byte array
	 * @throws IllegalArgumentException if a valid {@link Inet4Address} can not be created
	 */
	private static Inet4Address getInet4Address(byte[] bytes) {
		checkArgument(
				bytes.length == 4,
				"Byte array has invalid length for an IPv4 address: %s != 4.",
				bytes.length);

		// Given a 4-byte array, this cast should always succeed.
		return (Inet4Address) bytesToInetAddress(bytes);
	}

	/**
	 * Returns the {@link InetAddress} having the given string representation.
	 *
	 * <p>This deliberately avoids all nameservice lookups (e.g. no DNS).
	 *
	 * @param ipString {@code String} containing an IPv4 or IPv6 string literal, e.g. {@code "192.168.0.1"} or {@code
	 *                 "2001:db8::1"}
	 * @return {@link InetAddress} representing the argument
	 * @throws IllegalArgumentException if the argument is not a valid IP string literal
	 */
	public static InetAddress forString(String ipString) {
		byte[] addr = ipStringToBytes(ipString);

		// The argument was malformed, i.e. not an IP string literal.
		if (addr == null) {
			throw formatIllegalArgumentException("'%s' is not an IP string literal.", ipString);
		}

		return bytesToInetAddress(addr);
	}

	private static byte[] ipStringToBytes(String ipString) {
		// Make a first pass to categorize the characters in this string.
		boolean hasColon = false;
		boolean hasDot = false;
		for (int i = 0; i < ipString.length(); i++) {
			char c = ipString.charAt(i);
			if (c == '.') {
				hasDot = true;
			} else if (c == ':') {
				if (hasDot) {
					return null; // Colons must not appear after dots.
				}
				hasColon = true;
			} else if (Character.digit(c, 16) == -1) {
				return null; // Everything else must be a decimal or hex digit.
			}
		}

		// Now decide which address family to parse.
		if (hasColon) {
			if (hasDot) {
				ipString = convertDottedQuadToHex(ipString);
				if (ipString == null) {
					return null;
				}
			}
			return textToNumericFormatV6(ipString);
		} else if (hasDot) {
			return textToNumericFormatV4(ipString);
		}
		return null;
	}

	private static byte[] textToNumericFormatV4(String ipString) {
		byte[] bytes = new byte[IPV4_PART_COUNT];
		int i = 0;
		try {
			for (String octet : ipString.split("\\.", IPV4_PART_COUNT)) {
				bytes[i++] = parseOctet(octet);
			}
		} catch (NumberFormatException ex) {
			return null;
		}

		return i == IPV4_PART_COUNT ? bytes : null;
	}

	private static byte[] textToNumericFormatV6(String ipString) {
		// An address can have [2..8] colons, and N colons make N+1 parts.
		String[] parts = ipString.split(":", IPV6_PART_COUNT + 2);
		if (parts.length < 3 || parts.length > IPV6_PART_COUNT + 1) {
			return null;
		}

		// Disregarding the endpoints, find "::" with nothing in between.
		// This indicates that a run of zeroes has been skipped.
		int skipIndex = -1;
		for (int i = 1; i < parts.length - 1; i++) {
			if (parts[i].length() == 0) {
				if (skipIndex >= 0) {
					return null; // Can't have more than one ::
				}
				skipIndex = i;
			}
		}

		int partsHi; // Number of parts to copy from above/before the "::"
		int partsLo; // Number of parts to copy from below/after the "::"
		if (skipIndex >= 0) {
			// If we found a "::", then check if it also covers the endpoints.
			partsHi = skipIndex;
			partsLo = parts.length - skipIndex - 1;
			if (parts[0].length() == 0 && --partsHi != 0) {
				return null; // ^: requires ^::
			}
			if (parts[parts.length - 1].length() == 0 && --partsLo != 0) {
				return null; // :$ requires ::$
			}
		} else {
			// Otherwise, allocate the entire address to partsHi. The endpoints
			// could still be empty, but parseHextet() will check for that.
			partsHi = parts.length;
			partsLo = 0;
		}

		// If we found a ::, then we must have skipped at least one part.
		// Otherwise, we must have exactly the right number of parts.
		int partsSkipped = IPV6_PART_COUNT - (partsHi + partsLo);
		if (!(skipIndex >= 0 ? partsSkipped >= 1 : partsSkipped == 0)) {
			return null;
		}

		// Now parse the hextets into a byte array.
		ByteBuffer rawBytes = ByteBuffer.allocate(2 * IPV6_PART_COUNT);
		try {
			for (int i = 0; i < partsHi; i++) {
				rawBytes.putShort(parseHextet(parts[i]));
			}
			for (int i = 0; i < partsSkipped; i++) {
				rawBytes.putShort((short) 0);
			}
			for (int i = partsLo; i > 0; i--) {
				rawBytes.putShort(parseHextet(parts[parts.length - i]));
			}
		} catch (NumberFormatException ex) {
			return null;
		}
		return rawBytes.array();
	}

	private static String convertDottedQuadToHex(String ipString) {
		int lastColon = ipString.lastIndexOf(':');
		String initialPart = ipString.substring(0, lastColon + 1);
		String dottedQuad = ipString.substring(lastColon + 1);
		byte[] quad = textToNumericFormatV4(dottedQuad);
		if (quad == null) {
			return null;
		}
		String penultimate = Integer.toHexString(((quad[0] & 0xff) << 8) | (quad[1] & 0xff));
		String ultimate = Integer.toHexString(((quad[2] & 0xff) << 8) | (quad[3] & 0xff));
		return initialPart + penultimate + ":" + ultimate;
	}

	private static byte parseOctet(String ipPart) {
		// Note: we already verified that this string contains only hex digits.
		int octet = Integer.parseInt(ipPart);
		// Disallow leading zeroes, because no clear standard exists on
		// whether these should be interpreted as decimal or octal.
		if (octet > 255 || (ipPart.startsWith("0") && ipPart.length() > 1)) {
			throw new NumberFormatException();
		}
		return (byte) octet;
	}

	private static short parseHextet(String ipPart) {
		// Note: we already verified that this string contains only hex digits.
		int hextet = Integer.parseInt(ipPart, 16);
		if (hextet > 0xffff) {
			throw new NumberFormatException();
		}
		return (short) hextet;
	}

	/**
	 * Convert a byte array into an InetAddress.
	 *
	 * {@link InetAddress#getByAddress} is documented as throwing a checked exception "if IP address is of illegal
	 * length." We replace it with an unchecked exception, for use by callers who already know that addr is an array of
	 * length 4 or 16.
	 *
	 * @param addr the raw 4-byte or 16-byte IP address in big-endian order
	 * @return an InetAddress object created from the raw IP address
	 */
	private static InetAddress bytesToInetAddress(byte[] addr) {
		try {
			return InetAddress.getByAddress(addr);
		} catch (UnknownHostException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Returns an Inet4Address having the integer value specified by the argument.
	 *
	 * @param address {@code int}, the 32bit integer address to be converted
	 * @return {@link Inet4Address} equivalent of the argument
	 */
	public static Inet4Address fromInteger(int address) {
		return getInet4Address(Ints.toByteArray(address));
	}


	public static int inetAddressToInt(Inet4Address clientIp) {
		return ByteBuffer.wrap(clientIp.getAddress()).getInt();
	}

	private static IllegalArgumentException formatIllegalArgumentException(String format, Object... args) {
		return new IllegalArgumentException(String.format(Locale.ROOT, format, args));
	}
}
