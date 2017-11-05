package org.stagemonitor.web.servlet.eum;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.tracing.B3HeaderFormat;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.reporter.ReportingSpanEventListener;
import org.stagemonitor.tracing.tracing.B3Propagator;
import org.stagemonitor.tracing.wrapper.SpanWrappingTracer;
import org.stagemonitor.web.servlet.ServletPlugin;
import org.stagemonitor.web.servlet.eum.ClientSpanMetadataTagProcessor.ClientSpanMetadataDefinition;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.ThreadLocalScopeManager;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_APP_CACHE_LOOKUP;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_DNS_LOOKUP;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_LOAD;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_PROCESSING;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_REDIRECT;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_REQUEST;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_RESOURCE;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_RESPONSE;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_TCP;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_TIME_TO_FIRST_PAINT;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_UNLOAD;

public class ClientSpanServletTest {

	private MockTracer mockTracer;
	private ClientSpanServlet servlet;
	private ServletPlugin servletPlugin;
	private ReportingSpanEventListener reportingSpanEventListener;
	private TracingPlugin tracingPlugin;

	@Before
	public void setUp() {
		mockTracer = new MockTracer(new ThreadLocalScopeManager(), new B3Propagator());
		tracingPlugin = mock(TracingPlugin.class);
		SpanWrappingTracer spanWrappingTracer = new SpanWrappingTracer(mockTracer, asList(
				new SpanContextInformation.SpanContextSpanEventListener(),
				new SpanContextInformation.SpanFinalizer()));
		when(tracingPlugin.getTracer()).thenReturn(spanWrappingTracer);
		when(tracingPlugin.isSampled(any())).thenReturn(true);
		reportingSpanEventListener = mock(ReportingSpanEventListener.class);
		when(tracingPlugin.getReportingSpanEventListener()).thenReturn(reportingSpanEventListener);
		servletPlugin = mock(ServletPlugin.class);
		when(servletPlugin.isClientSpanCollectionEnabled()).thenReturn(true);
		when(servletPlugin.isParseUserAgent()).thenReturn(false);
		servlet = new ClientSpanServlet(tracingPlugin, servletPlugin);
	}

	@Test
	public void testConvertWeaselBeaconToSpan_withPageLoadBeacon() throws ServletException, IOException {
		// Given
		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		mockHttpServletRequest.setParameter("ty", "pl");
		mockHttpServletRequest.setParameter("r", "1496751574200");
		mockHttpServletRequest.setParameter("u", "http://localhost:9966/petclinic/");
		mockHttpServletRequest.setParameter("m_user", "tom.mason@example.com");
		mockHttpServletRequest.setParameter("ts", "-197");
		mockHttpServletRequest.setParameter("d", "518");
		mockHttpServletRequest.setParameter("t_unl", "0");
		mockHttpServletRequest.setParameter("t_red", "0");
		mockHttpServletRequest.setParameter("t_apc", "5");
		mockHttpServletRequest.setParameter("t_dns", "0");
		mockHttpServletRequest.setParameter("t_tcp", "0");
		mockHttpServletRequest.setParameter("t_req", "38");
		mockHttpServletRequest.setParameter("t_rsp", "4");
		mockHttpServletRequest.setParameter("t_pro", "471");
		mockHttpServletRequest.setParameter("t_loa", "5");
		mockHttpServletRequest.setParameter("t_fp", "151");

		// TODO, ignore for now
		mockHttpServletRequest.setParameter("k", "someKey"); // not necessary
		mockHttpServletRequest.setParameter("t", "a6b5fd025be24191"); // trace id -> opentracing does not specify this yet
		mockHttpServletRequest.setParameter("res", "{\"http://localhost:9966/petclinic/\":{\"webjars/\":{\"bootstrap/2.3.0/\":{\"css/bootstrap.min.css\":[\"-136,10,2,1,105939\"],\"img/glyphicons-halflings.png\":[\"-48,0,4,1,12799\"]},\"jquery\":{\"/2.0.3/jquery.js\":[\"-136,0,3,1,242142\"],\"-ui/1.10.3/\":{\"ui/jquery.ui.\":{\"core.js\":[\"-136,0,3,1,8198\"],\"datepicker.js\":[\"-136,0,3,1,76324\"]},\"themes/base/jquery-ui.css\":[\"-136,8,2,1,32456\"]}}},\"resources/\":{\"css/petclinic.css\":[\"-136,8,2,1,243\"],\"images/\":{\"banner-graphic.png\":[\"-136,0,1,1,13773\"],\"pets.png\":[\"-136,0,1,1,55318\"],\"spring-pivotal-logo.png\":[\"-136,0,1,1,2818\"]}},\"stagemonitor/\":{\"public/static/\":{\"rum/boomerang-56c823668fc.min.js\":[\"-136,0,3,1,12165\"],\"eum.debug.js\":[\"-32,12,3,3,23798\"]},\"static/stagemonitor\":{\".png\":[\"-136,16,1,3,1694\"],\"-modal.html\":[\"-33,9,5,3,10538\"]}}}}");

		// When
		servlet.doPost(mockHttpServletRequest, new MockHttpServletResponse());

		// Then
		verify(reportingSpanEventListener, never()).update(any(), any(), any());
		assertSoftly(softly -> {
			final List<MockSpan> finishedSpans = mockTracer.finishedSpans();
			softly.assertThat(finishedSpans).hasSize(1);
			MockSpan span = finishedSpans.get(0);
			softly.assertThat(span.operationName()).isEqualTo("/petclinic/");
			softly.assertThat(span.finishMicros() - span.startMicros()).isEqualTo(TimeUnit.MILLISECONDS.toMicros(518L));

			final long redirect = 0L;
			final long appCacheLookup = 5L;
			final long dns = 0L;
			final long tcp = 0L;
			final long request = 38L;
			final long response = 4L;
			softly.assertThat(span.tags())
					.containsEntry("type", "pageload")
					.doesNotContainEntry(Tags.SAMPLING_PRIORITY.getKey(), 0)
					.doesNotContainKey("user")
					.containsEntry("http.url", "http://localhost:9966/petclinic/")
					.containsEntry("timing.unload", 0L)
					.containsEntry("timing.redirect", redirect)
					.containsEntry("timing.app_cache_lookup", appCacheLookup)
					.containsEntry("timing.dns_lookup", dns)
					.containsEntry("timing.tcp", tcp)
					.containsEntry("timing.request", request)
					.containsEntry("timing.response", response)
					.containsEntry("timing.processing", 471L)
					.containsEntry("timing.load", 5L)
					.containsEntry("timing.time_to_first_paint", 151L)
					.containsEntry("timing.resource", redirect + appCacheLookup + dns + tcp + request + response);
		});
	}

	@Test
	public void testConvertWeaselBeaconToSpan_withPageLoadBeacon_withBackendTraceId() throws ServletException, IOException {
		// Given
		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		mockHttpServletRequest.setParameter("ty", "pl");
		mockHttpServletRequest.setParameter("d", "518");
		mockHttpServletRequest.setParameter("r", "1496751574200");
		mockHttpServletRequest.setParameter("ts", "-197");

		mockHttpServletRequest.setParameter("t", "a6b5fd025be24191"); // trace id -> opentracing does not specify this yet
		final String backendTraceId = "6210be349041096e";
		mockHttpServletRequest.setParameter("bt", backendTraceId);
		final String backendSpanId = "6210be349041096e";
		mockHttpServletRequest.setParameter("m_bs", backendSpanId);

		// When
		servlet.doPost(mockHttpServletRequest, new MockHttpServletResponse());

		// Then
		final List<MockSpan> finishedSpans = mockTracer.finishedSpans();
		assertThat(finishedSpans).hasSize(1);
		MockSpan span = finishedSpans.get(0);
		final String spanIdOfPageloadTrace = B3HeaderFormat.getB3Identifiers(mockTracer, span).getSpanId();
		final B3HeaderFormat.B3Identifiers traceIdsOfServerSpan = B3HeaderFormat.B3Identifiers.builder()
				.traceId(backendTraceId)
				.spanId(backendSpanId)
				.build();
		final B3HeaderFormat.B3Identifiers newSpanIdentifiers = B3HeaderFormat.B3Identifiers.builder()
				.traceId(backendTraceId)
				.spanId(backendSpanId)
				.parentSpanId(spanIdOfPageloadTrace)
				.build();
		verify(reportingSpanEventListener).update(eq(traceIdsOfServerSpan), eq(newSpanIdentifiers), eq(Collections.emptyMap()));
		assertSoftly(softly -> softly.assertThat(span.tags())
				.containsEntry("type", "pageload")
				// TODO test via B3HeaderFormat.getTraceId(mockTracer, span); when https://github.com/opentracing/specification/issues/81 is resolved
				.containsEntry("trace_id", backendTraceId)
				.doesNotContainEntry(Tags.SAMPLING_PRIORITY.getKey(), 0));
	}

	@Test
	public void testConvertWeaselBeaconToSpan_withNegativeRedirectTimeIsDiscarded() throws ServletException, IOException {
		// Given
		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		mockHttpServletRequest.setParameter("ty", "pl");
		mockHttpServletRequest.setParameter("r", "1496751574200");
		mockHttpServletRequest.setParameter("u", "http://localhost:9966/petclinic/");
		mockHttpServletRequest.setParameter("ts", "-197");
		mockHttpServletRequest.setParameter("d", "518");
		mockHttpServletRequest.setParameter("t_unl", "0");
		mockHttpServletRequest.setParameter("t_red", "-500");
		mockHttpServletRequest.setParameter("t_apc", "5");
		mockHttpServletRequest.setParameter("t_dns", "0");
		mockHttpServletRequest.setParameter("t_tcp", "0");
		mockHttpServletRequest.setParameter("t_req", "38");
		mockHttpServletRequest.setParameter("t_rsp", "4");
		mockHttpServletRequest.setParameter("t_pro", "471");
		mockHttpServletRequest.setParameter("t_loa", "5");
		mockHttpServletRequest.setParameter("t_fp", "151");

		// When
		servlet.doPost(mockHttpServletRequest, new MockHttpServletResponse());

		// Then
		assertSoftly(softly -> {
			final List<MockSpan> finishedSpans = mockTracer.finishedSpans();
			softly.assertThat(finishedSpans).hasSize(1);
			MockSpan span = finishedSpans.get(0);
			softly.assertThat(span.tags())
					.containsEntry("type", "pageload")
					.containsEntry(Tags.SAMPLING_PRIORITY.getKey(), 0);
		});
	}


	@Test
	public void testConvertWeaselBeaconToSpan_skipsTagProcessorsIfSpanIsNotSampled() throws ServletException, IOException {
		// Given
		when(tracingPlugin.isSampled(any())).thenReturn(false);

		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		mockHttpServletRequest.setParameter("ty", "pl");
		mockHttpServletRequest.setParameter("r", "1496751574200");
		mockHttpServletRequest.setParameter("u", "http://localhost:9966/petclinic/");
		mockHttpServletRequest.setParameter("m_user", "tom.mason@example.com");
		mockHttpServletRequest.setParameter("ts", "-197");
		mockHttpServletRequest.setParameter("d", "518");
		mockHttpServletRequest.setParameter("t_unl", "0");
		mockHttpServletRequest.setParameter("t_red", "0");
		mockHttpServletRequest.setParameter("t_apc", "5");
		mockHttpServletRequest.setParameter("t_dns", "0");
		mockHttpServletRequest.setParameter("t_tcp", "0");
		mockHttpServletRequest.setParameter("t_req", "38");
		mockHttpServletRequest.setParameter("t_rsp", "4");
		mockHttpServletRequest.setParameter("t_pro", "471");
		mockHttpServletRequest.setParameter("t_loa", "5");
		mockHttpServletRequest.setParameter("t_fp", "151");

		// When
		servlet.doPost(mockHttpServletRequest, new MockHttpServletResponse());

		// Then
		assertSoftly(softly -> {
			final List<MockSpan> finishedSpans = mockTracer.finishedSpans();
			softly.assertThat(finishedSpans).hasSize(1);
			MockSpan span = finishedSpans.get(0);
			softly.assertThat(span.tags())
					.containsEntry("type", "pageload")
					.doesNotContainKeys(TIMING_UNLOAD, TIMING_REDIRECT, TIMING_APP_CACHE_LOOKUP, TIMING_DNS_LOOKUP,
							TIMING_TCP, TIMING_REQUEST, TIMING_RESPONSE, TIMING_PROCESSING, TIMING_LOAD,
							TIMING_TIME_TO_FIRST_PAINT, TIMING_RESOURCE);
		});
	}

	@Test
	public void testConvertWeaselBeaconToSpan_withMetadata() throws ServletException, IOException {
		// Given - normal trace data
		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		mockHttpServletRequest.setParameter("ty", "pl");
		mockHttpServletRequest.setParameter("r", "1496751574200");
		mockHttpServletRequest.setParameter("u", "http://localhost:9966/petclinic/");
		mockHttpServletRequest.setParameter("m_user", "tom.mason@example.com");
		mockHttpServletRequest.setParameter("ts", "-197");
		mockHttpServletRequest.setParameter("d", "518");
		mockHttpServletRequest.setParameter("t_unl", "0");
		mockHttpServletRequest.setParameter("t_red", "0");
		mockHttpServletRequest.setParameter("t_apc", "5");
		mockHttpServletRequest.setParameter("t_dns", "0");
		mockHttpServletRequest.setParameter("t_tcp", "0");
		mockHttpServletRequest.setParameter("t_req", "38");
		mockHttpServletRequest.setParameter("t_rsp", "4");
		mockHttpServletRequest.setParameter("t_pro", "471");
		mockHttpServletRequest.setParameter("t_loa", "5");
		mockHttpServletRequest.setParameter("t_fp", "151");

		// Given - metadata
		mockHttpServletRequest.setParameter("m_username", "test string here");
		mockHttpServletRequest.setParameter("m_age", "26");
		mockHttpServletRequest.setParameter("m_is_access_allowed", "1");
		mockHttpServletRequest.setParameter("m_some_not_mapped_property", "this should not exist");

		// When
		final HashMap<String, ClientSpanMetadataDefinition> whitelistedValues = new HashMap<>();
		whitelistedValues.put("username", new ClientSpanMetadataDefinition("string"));
		whitelistedValues.put("age", new ClientSpanMetadataDefinition("number"));
		whitelistedValues.put("is_access_allowed", new ClientSpanMetadataDefinition("boolean"));
		whitelistedValues.put("parameter_not_sent", new ClientSpanMetadataDefinition("string"));
		when(servletPlugin.getWhitelistedClientSpanTags()).thenReturn(whitelistedValues);
		servlet.doGet(mockHttpServletRequest, new MockHttpServletResponse());

		// Then
		assertSoftly(softly -> {
			final List<MockSpan> finishedSpans = mockTracer.finishedSpans();
			softly.assertThat(finishedSpans).hasSize(1);
			MockSpan span = finishedSpans.get(0);

			softly.assertThat(span.operationName()).isEqualTo("/petclinic/");

			softly.assertThat(span.tags())
					.doesNotContainEntry(Tags.SAMPLING_PRIORITY.getKey(), 0)
					.containsEntry("type", "pageload")
					.containsEntry("username", "test string here")
					.containsEntry("age", 26.)
					.containsEntry("is_access_allowed", true)
					.doesNotContainKey("parameter_not_sent")
					.doesNotContainKey("some_not_mapped_property");
		});
	}

	@Test
	public void testConvertWeaselBeaconToSpan_withErrorBeacon() throws ServletException, IOException {
		// Given
		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		mockHttpServletRequest.setParameter("k", "someKey");
		mockHttpServletRequest.setParameter("s", "3776086ebf658768");
		mockHttpServletRequest.setParameter("t", "3776086ebf658768");
		mockHttpServletRequest.setParameter("ts", "1496753245024");
		mockHttpServletRequest.setParameter("ty", "err");
		mockHttpServletRequest.setParameter("pl", "e6bf60fdf2672398");
		mockHttpServletRequest.setParameter("l", "http://localhost:9966/petclinic/");
		mockHttpServletRequest.setParameter("e", "Uncaught null");
		mockHttpServletRequest.setParameter("st", "at http://localhost:9966/petclinic/ 301:34");
		mockHttpServletRequest.setParameter("c", "1");
		mockHttpServletRequest.setParameter("m_user", "tom.mason@example.com");

		// When
		servlet.doGet(mockHttpServletRequest, new MockHttpServletResponse());

		// Then
		assertSoftly(softly -> {
			final List<MockSpan> finishedSpans = mockTracer.finishedSpans();
			softly.assertThat(finishedSpans).hasSize(1);
			MockSpan span = finishedSpans.get(0);
			softly.assertThat(span.operationName()).isEqualTo("/petclinic/");
			softly.assertThat(span.finishMicros() - span.startMicros()).isZero();

			softly.assertThat(span.tags())
					.doesNotContainEntry(Tags.SAMPLING_PRIORITY.getKey(), 0)
					.containsEntry("http.url", "http://localhost:9966/petclinic/")
					.containsEntry("type", "js_error")
					.containsEntry("exception.stack_trace", "at http://localhost:9966/petclinic/ 301:34")
					.containsEntry("exception.message", "Uncaught null");
		});
	}

	@Test
	public void testConvertWeaselBeaconToSpan_withErrorBeaconTrimsTooLongStackTraces() throws ServletException, IOException {
		// Given
		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		mockHttpServletRequest.setParameter("k", "someKey");
		mockHttpServletRequest.setParameter("s", "3776086ebf658768");
		mockHttpServletRequest.setParameter("t", "3776086ebf658768");
		mockHttpServletRequest.setParameter("ts", "1496753245024");
		mockHttpServletRequest.setParameter("ty", "err");
		mockHttpServletRequest.setParameter("pl", "e6bf60fdf2672398");
		mockHttpServletRequest.setParameter("l", "http://localhost:9966/petclinic/");
		mockHttpServletRequest.setParameter("e", "Uncaught null");
		final StringBuilder stacktrace = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			stacktrace.append("at http://localhost:9966/petclinic/ 301:34\n");
		}
		mockHttpServletRequest.setParameter("st", stacktrace.toString());
		mockHttpServletRequest.setParameter("c", "1");
		mockHttpServletRequest.setParameter("m_user", "tom.mason@example.com");

		// When
		servlet.doGet(mockHttpServletRequest, new MockHttpServletResponse());

		// Then
		assertSoftly(softly -> {
			final List<MockSpan> finishedSpans = mockTracer.finishedSpans();
			softly.assertThat(finishedSpans).hasSize(1);
			MockSpan span = finishedSpans.get(0);
			softly.assertThat(span.operationName()).isEqualTo("/petclinic/");
			softly.assertThat(span.finishMicros() - span.startMicros()).isZero();

			softly.assertThat(span.tags())
					.doesNotContainEntry(Tags.SAMPLING_PRIORITY.getKey(), 0)
					.containsEntry("http.url", "http://localhost:9966/petclinic/")
					.containsEntry("type", "js_error")
					.containsKey("exception.stack_trace")
					.containsEntry("exception.message", "Uncaught null");

			softly.assertThat(((String) span.tags().get("exception.stack_trace")).length())
					.isLessThan(stacktrace.length())
					.isGreaterThan(0);
		});
	}

	@Test
	public void testConvertWeaselBeaconToSpan_withXHRBeacon() throws ServletException, IOException {
		// Given
		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		mockHttpServletRequest.setParameter("r", "1496994284184");
		mockHttpServletRequest.setParameter("k", "null");
		mockHttpServletRequest.setParameter("ts", "21793");
		mockHttpServletRequest.setParameter("d", "2084");
		mockHttpServletRequest.setParameter("ty", "xhr");
		mockHttpServletRequest.setParameter("pl", "d58cddae830273d1");
		mockHttpServletRequest.setParameter("l", "http://localhost:9966/petclinic/");
		mockHttpServletRequest.setParameter("m", "GET");
		mockHttpServletRequest.setParameter("u", "http://localhost:9966/petclinic/owners.html?lastName=");
		mockHttpServletRequest.setParameter("a", "1");
		mockHttpServletRequest.setParameter("st", "200");
		mockHttpServletRequest.setParameter("e", "undefined");
		mockHttpServletRequest.setParameter("m_user", "tom.mason@example.com");
		mockHttpServletRequest.setParameter("t", "2d371455215c504");
		mockHttpServletRequest.setParameter("s", "2d371455215c504");

		// When
		servlet.doGet(mockHttpServletRequest, new MockHttpServletResponse());

		// Then
		assertSoftly(softly -> {
			final List<MockSpan> finishedSpans = mockTracer.finishedSpans();
			softly.assertThat(finishedSpans).hasSize(1);
			MockSpan span = finishedSpans.get(0);
			softly.assertThat(span.operationName()).isEqualTo("/petclinic/owners.html");
			softly.assertThat(span.finishMicros() - span.startMicros()).isEqualTo(TimeUnit.MILLISECONDS.toMicros(2084L));

			softly.assertThat(span.tags())
					.doesNotContainEntry(Tags.SAMPLING_PRIORITY.getKey(), 0)
					.containsEntry("type", "ajax")
					.containsEntry("http.status_code", 200L)
					.containsEntry("method", "GET")
					.containsEntry("xhr.requested_url", "http://localhost:9966/petclinic/owners.html?lastName=")
					.containsEntry("xhr.requested_from", "http://localhost:9966/petclinic/")
					.containsEntry("xhr.async", true)
					.containsEntry("duration_ms", 2084L)
					.containsEntry("id", "2d371455215c504")
					.containsEntry("trace_id", "2d371455215c504");
		});
	}

}
