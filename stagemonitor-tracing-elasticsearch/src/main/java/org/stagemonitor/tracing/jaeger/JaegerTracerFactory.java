package org.stagemonitor.tracing.jaeger;

import com.uber.jaeger.SpanContext;
import com.uber.jaeger.propagation.Extractor;
import com.uber.jaeger.propagation.Injector;
import com.uber.jaeger.reporters.NoopReporter;
import com.uber.jaeger.samplers.ConstSampler;

import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.tracing.B3HeaderFormat;
import org.stagemonitor.tracing.TracerFactory;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;

public class JaegerTracerFactory extends TracerFactory {

	@Override
	public Tracer getTracer(StagemonitorPlugin.InitArguments initArguments) {
		final B3TextMapCodec b3TextMapCodec = new B3TextMapCodec();
		final com.uber.jaeger.Tracer.Builder builder = new com.uber.jaeger.Tracer.Builder(
				initArguments.getMeasurementSession().getApplicationName(),
				new NoopReporter(),
				new ConstSampler(true))
				.registerInjector(B3HeaderFormat.INSTANCE, b3TextMapCodec)
				.registerInjector(Format.Builtin.HTTP_HEADERS, b3TextMapCodec)
				.registerExtractor(Format.Builtin.HTTP_HEADERS, b3TextMapCodec);
		return builder.build();
	}

	// TODO use jaeger-b3 module
	/*
	 * Copyright (c) 2016, Uber Technologies, Inc
	 *
	 * Permission is hereby granted, free of charge, to any person obtaining a copy
	 * of this software and associated documentation files (the "Software"), to deal
	 * in the Software without restriction, including without limitation the rights
	 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	 * copies of the Software, and to permit persons to whom the Software is
	 * furnished to do so, subject to the following conditions:
	 *
	 * The above copyright notice and this permission notice shall be included in
	 * all copies or substantial portions of the Software.
	 *
	 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	 * THE SOFTWARE.
	 */

	/**
	 * This format is compatible with other trace libraries such as Brave, Wingtips, zipkin-js, etc.
	 *
	 * <p>
	 * Example usage:
	 *
	 * <pre>{@code
	 * b3Codec = new B3TextMapCodec();
	 * tracer = new Tracer.Builder(serviceName, reporter, sampler)
	 *                    .registerInjector(Format.Builtin.HTTP_HEADERS, b3Codec)
	 *                    .registerExtractor(Format.Builtin.HTTP_HEADERS, b3Codec)
	 *                    ...
	 * }</pre>
	 *
	 * <p>
	 * See <a href="http://zipkin.io/pages/instrumenting.html">Instrumenting a Library</a>
	 */
	private static final class B3TextMapCodec implements Injector<TextMap>, Extractor<TextMap> {
		static final String TRACE_ID_NAME = "X-B3-TraceId";
		static final String SPAN_ID_NAME = "X-B3-SpanId";
		static final String PARENT_SPAN_ID_NAME = "X-B3-ParentSpanId";
		static final String SAMPLED_NAME = "X-B3-Sampled";
		static final String FLAGS_NAME = "X-B3-Flags";
		// NOTE: uber's flags aren't the same as B3/Finagle ones
		static final byte SAMPLED_FLAG = 1;
		static final byte DEBUG_FLAG = 2;

		@Override
		public void inject(SpanContext spanContext, TextMap carrier) {
			carrier.put(TRACE_ID_NAME, Util.toLowerHex(spanContext.getTraceID()));
			if (spanContext.getParentID() != 0L) { // Conventionally, parent id == 0 means the root span
				carrier.put(PARENT_SPAN_ID_NAME, Util.toLowerHex(spanContext.getParentID()));
			}
			carrier.put(SPAN_ID_NAME, Util.toLowerHex(spanContext.getSpanID()));
			carrier.put(SAMPLED_NAME, spanContext.isSampled() ? "1" : "0");
			if (spanContext.isDebug()) {
				carrier.put(FLAGS_NAME, "1");
			}
		}

		@Override
		public SpanContext extract(TextMap carrier) {
			Long traceID = null;
			Long spanID = null;
			long parentID = 0L; // Conventionally, parent id == 0 means the root span
			byte flags = 0;
			for (Map.Entry<String, String> entry : carrier) {
				if (entry.getKey().equalsIgnoreCase(SAMPLED_NAME)) {
					if (entry.getValue().equals("1") || entry.getValue().toLowerCase().equals("true")) {
						flags |= SAMPLED_FLAG;
					}
				} else if (entry.getKey().equalsIgnoreCase(TRACE_ID_NAME)) {
					traceID = Util.lowerHexToUnsignedLong(entry.getValue());
				} else if (entry.getKey().equalsIgnoreCase(PARENT_SPAN_ID_NAME)) {
					parentID = Util.lowerHexToUnsignedLong(entry.getValue());
				} else if (entry.getKey().equalsIgnoreCase(SPAN_ID_NAME)) {
					spanID = Util.lowerHexToUnsignedLong(entry.getValue());
				} else if (entry.getKey().equalsIgnoreCase(FLAGS_NAME)) {
					if (entry.getValue().equals("1")) {
						flags |= DEBUG_FLAG;
					}
				}
			}

			if (traceID != null && spanID != null) {
				return new SpanContext(traceID, spanID, parentID, flags);
			}
			return null;
		}
	}

	/**
	 * Copyright 2015-2017 The OpenZipkin Authors
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
	private static final class Util {
		public static final Charset UTF_8 = Charset.forName("UTF-8");
		static final TimeZone UTC = TimeZone.getTimeZone("UTC");

		public static int envOr(String key, int fallback) {
			return System.getenv(key) != null ? Integer.parseInt(System.getenv(key)) : fallback;
		}

		public static String envOr(String key, String fallback) {
			return System.getenv(key) != null ? System.getenv(key) : fallback;
		}

		public static boolean equal(Object a, Object b) {
			return a == b || (a != null && a.equals(b));
		}

		/**
		 * Copy of {@code com.google.common.base.Preconditions#checkArgument}.
		 */
		public static void checkArgument(boolean expression,
										 String errorMessageTemplate,
										 Object... errorMessageArgs) {
			if (!expression) {
				throw new IllegalArgumentException(String.format(errorMessageTemplate, errorMessageArgs));
			}
		}

		/**
		 * Copy of {@code com.google.common.base.Preconditions#checkNotNull}.
		 */
		public static <T> T checkNotNull(T reference, String errorMessage) {
			if (reference == null) {
				// If either of these parameters is null, the right thing happens anyway
				throw new NullPointerException(errorMessage);
			}
			return reference;
		}

		public static <T extends Comparable<? super T>> List<T> sortedList(Collection<T> in) {
			if (in == null || in.isEmpty()) return Collections.emptyList();
			if (in.size() == 1) return Collections.singletonList(in.iterator().next());
			Object[] array = in.toArray();
			Arrays.sort(array);
			List result = Arrays.asList(array);
			return Collections.unmodifiableList(result);
		}

		/** For bucketed data floored to the day. For example, dependency links. */
		public static long midnightUTC(long epochMillis) {
			Calendar day = Calendar.getInstance(UTC);
			day.setTimeInMillis(epochMillis);
			day.set(Calendar.MILLISECOND, 0);
			day.set(Calendar.SECOND, 0);
			day.set(Calendar.MINUTE, 0);
			day.set(Calendar.HOUR_OF_DAY, 0);
			return day.getTimeInMillis();
		}

		public static List<Date> getDays(long endTs, Long lookback) {
			long to = midnightUTC(endTs);
			long from = midnightUTC(endTs - (lookback != null ? lookback : endTs));

			List<Date> days = new ArrayList<Date>();
			for (long time = from; time <= to; time += TimeUnit.DAYS.toMillis(1)) {
				days.add(new Date(time));
			}
			return days;
		}

		/**
		 * Parses a 1 to 32 character lower-hex string with no prefix into an unsigned long, tossing any
		 * bits higher than 64.
		 */
		public static long lowerHexToUnsignedLong(String lowerHex) {
			int length = lowerHex.length();
			if (length < 1 || length > 32) throw isntLowerHexLong(lowerHex);

			// trim off any high bits
			int beginIndex = length > 16 ? length - 16 : 0;

			return lowerHexToUnsignedLong(lowerHex, beginIndex);
		}

		/**
		 * Parses a 16 character lower-hex string with no prefix into an unsigned long, starting at the
		 * spe index.
		 */
		public static long lowerHexToUnsignedLong(String lowerHex, int index) {
			long result = 0;
			for (int endIndex = Math.min(index + 16, lowerHex.length()); index < endIndex; index++) {
				char c = lowerHex.charAt(index);
				result <<= 4;
				if (c >= '0' && c <= '9') {
					result |= c - '0';
				} else if (c >= 'a' && c <= 'f') {
					result |= c - 'a' + 10;
				} else {
					throw isntLowerHexLong(lowerHex);
				}
			}
			return result;
		}

		static NumberFormatException isntLowerHexLong(String lowerHex) {
			throw new NumberFormatException(
					lowerHex + " should be a 1 to 32 character lower-hex string with no prefix");
		}

		/** Returns 16 or 32 character hex string depending on if {@code high} is zero. */
		public static String toLowerHex(long high, long low) {
			char[] result = new char[high != 0 ? 32 : 16];
			int pos = 0;
			if (high != 0) {
				writeHexLong(result, pos, high);
				pos += 16;
			}
			writeHexLong(result, pos, low);
			return new String(result);
		}

		/** Inspired by {@code okio.Buffer.writeLong} */
		public static String toLowerHex(long v) {
			char[] data = new char[16];
			writeHexLong(data, 0, v);
			return new String(data);
		}

		/** Inspired by {@code okio.Buffer.writeLong} */
		public static void writeHexLong(char[] data, int pos, long v) {
			writeHexByte(data, pos + 0,  (byte) ((v >>> 56L) & 0xff));
			writeHexByte(data, pos + 2,  (byte) ((v >>> 48L) & 0xff));
			writeHexByte(data, pos + 4,  (byte) ((v >>> 40L) & 0xff));
			writeHexByte(data, pos + 6,  (byte) ((v >>> 32L) & 0xff));
			writeHexByte(data, pos + 8,  (byte) ((v >>> 24L) & 0xff));
			writeHexByte(data, pos + 10, (byte) ((v >>> 16L) & 0xff));
			writeHexByte(data, pos + 12, (byte) ((v >>> 8L) & 0xff));
			writeHexByte(data, pos + 14, (byte)  (v & 0xff));
		}

		// Taken from RxJava throwIfFatal, which was taken from scala
		public static void propagateIfFatal(Throwable t) {
			if (t instanceof VirtualMachineError) {
				throw (VirtualMachineError) t;
			} else if (t instanceof ThreadDeath) {
				throw (ThreadDeath) t;
			} else if (t instanceof LinkageError) {
				throw (LinkageError) t;
			}
		}

		static final char[] HEX_DIGITS =
				{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

		static void writeHexByte(char[] data, int pos, byte b) {
			data[pos + 0] = HEX_DIGITS[(b >> 4) & 0xf];
			data[pos + 1] = HEX_DIGITS[b & 0xf];
		}

		// throwable ctor not present in JRE 6
		static AssertionError assertionError(String message, Throwable cause) {
			AssertionError error = new AssertionError(message);
			error.initCause(cause);
			throw error;
		}

		private Util() {
		}
	}

}
