package org.stagemonitor.web.servlet.eum;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.web.servlet.ServletPlugin;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClientSpanServletTest {

	private MockTracer tracer;
	private ClientSpanServlet servlet;

	@Test
	public void testConvertWeaselTraceToStagemonitorTrace_withPageLoadBeacon() {
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
		mockHttpServletRequest.setParameter("bt", "null"); // not sure if necessary
		mockHttpServletRequest.setParameter("res", "{\"http://localhost:9966/petclinic/\":{\"webjars/\":{\"bootstrap/2.3.0/\":{\"css/bootstrap.min.css\":[\"-136,10,2,1,105939\"],\"img/glyphicons-halflings.png\":[\"-48,0,4,1,12799\"]},\"jquery\":{\"/2.0.3/jquery.js\":[\"-136,0,3,1,242142\"],\"-ui/1.10.3/\":{\"ui/jquery.ui.\":{\"core.js\":[\"-136,0,3,1,8198\"],\"datepicker.js\":[\"-136,0,3,1,76324\"]},\"themes/base/jquery-ui.css\":[\"-136,8,2,1,32456\"]}}},\"resources/\":{\"css/petclinic.css\":[\"-136,8,2,1,243\"],\"images/\":{\"banner-graphic.png\":[\"-136,0,1,1,13773\"],\"pets.png\":[\"-136,0,1,1,55318\"],\"spring-pivotal-logo.png\":[\"-136,0,1,1,2818\"]}},\"stagemonitor/\":{\"public/static/\":{\"rum/boomerang-56c823668fc.min.js\":[\"-136,0,3,1,12165\"],\"eum.debug.js\":[\"-32,12,3,3,23798\"]},\"static/stagemonitor\":{\".png\":[\"-136,16,1,3,1694\"],\"-modal.html\":[\"-33,9,5,3,10538\"]}}}}");

		// When
		servlet.convertWeaselTraceToStagemonitorTrace(mockHttpServletRequest);

		// Then
		final List<MockSpan> finishedSpans = tracer.finishedSpans();
		assertThat(finishedSpans).hasSize(1);
		MockSpan span = finishedSpans.get(0);
		assertThat(span.operationName()).isEqualTo("pl http://localhost:9966/petclinic/");
		assertThat(span.startMicros()).isEqualTo(TimeUnit.MILLISECONDS.toMicros(1496751574200L + -197L));
		assertThat(span.finishMicros()).isEqualTo(TimeUnit.MILLISECONDS.toMicros(1496751574200L + 518L));

		assertThat(span.tags().get("type")).isEqualTo("client_pageload");
		assertThat(span.tags().get("user")).isEqualTo(null); // TODO: test whitelisting of metatags

		assertThat(span.tags().get("timing.unload")).isEqualTo(0L);
		assertThat(span.tags().get("timing.redirect")).isEqualTo(0L);
		assertThat(span.tags().get("timing.app_cache_lookup")).isEqualTo(5L);
		assertThat(span.tags().get("timing.dns_lookup")).isEqualTo(0L);
		assertThat(span.tags().get("timing.tcp")).isEqualTo(0L);
		assertThat(span.tags().get("timing.time_to_first_byte")).isEqualTo(38L);
		assertThat(span.tags().get("timing.response")).isEqualTo(4L);
		assertThat(span.tags().get("timing.processing")).isEqualTo(471L);
		assertThat(span.tags().get("timing.load")).isEqualTo(5L);
		assertThat(span.tags().get("timing.time_to_first_paint")).isEqualTo(151L);
	}

	@Test
	public void testConvertWeaselTraceToStagemonitorTrace_withErrorBeacon() {
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
		servlet.convertWeaselTraceToStagemonitorTrace(mockHttpServletRequest);

		// Then
		final List<MockSpan> finishedSpans = tracer.finishedSpans();
		assertThat(finishedSpans).hasSize(1);
		MockSpan span = finishedSpans.get(0);
		assertThat(span.tags().get("type")).isEqualTo("client_error");
		assertThat(span.startMicros()).isEqualTo(1496753245024000L);
		assertThat(span.operationName()).isEqualTo("err http://localhost:9966/petclinic/");
	}

	@Test
	public void testConvertWeaselTraceToStagemonitorTrace_withXHRBeacon() {
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
		mockHttpServletRequest.setParameter("u", "owners.html?lastName=");
		mockHttpServletRequest.setParameter("a", "1");
		mockHttpServletRequest.setParameter("st", "200");
		mockHttpServletRequest.setParameter("e", "undefined");
		mockHttpServletRequest.setParameter("m_user", "tom.mason@example.com");
		mockHttpServletRequest.setParameter("t", "2d371455215c504");
		mockHttpServletRequest.setParameter("s", "2d371455215c504");

		// When
		servlet.convertWeaselTraceToStagemonitorTrace(mockHttpServletRequest);

		// Then
		final List<MockSpan> finishedSpans = tracer.finishedSpans();
		assertThat(finishedSpans).hasSize(1);
		MockSpan span = finishedSpans.get(0);
		assertThat(span.tags().get("type")).isEqualTo("client_ajax");
		assertThat(span.startMicros()).isEqualTo(1496994305977000L);
		assertThat(span.operationName()).isEqualTo("xhr http://localhost:9966/petclinic/");

	}

	@Before
	public void setUp() {
		tracer = new MockTracer();
		TracingPlugin tracingPlugin = mock(TracingPlugin.class);
		when(tracingPlugin.getTracer()).thenReturn(tracer);
		ServletPlugin webPlugin = mock(ServletPlugin.class);
		when(webPlugin.isParseUserAgent()).thenReturn(false);
		servlet = new ClientSpanServlet(tracingPlugin, webPlugin);
	}

}
