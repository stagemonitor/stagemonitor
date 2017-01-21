package org.stagemonitor.web.monitor.filter.htmlinjector;

import com.uber.jaeger.reporters.NoopReporter;
import com.uber.jaeger.samplers.ConstSampler;

import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.IOUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.utils.SpanUtils;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.MonitoredHttpRequest;
import org.stagemonitor.web.monitor.filter.HtmlInjector;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;
import org.stagemonitor.web.monitor.rum.BoomerangJsHtmlInjector;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BoomerangJsHtmlInjectorTest {

	@Test
	public void testBommerangJsExistsAndHashIsCorrect() throws Exception {
		final String location = "/stagemonitor/public/static/rum/" + BoomerangJsHtmlInjector.BOOMERANG_FILENAME;
		final InputStream inputStream = getClass().getResourceAsStream(location);
		assertNotNull(inputStream);

		String contentHash = StringUtils.sha1Hash(IOUtils.toString(inputStream).replace("\r\n", "\n")).substring(0, 11);
		assertEquals("boomerang-" + contentHash + ".min.js", BoomerangJsHtmlInjector.BOOMERANG_FILENAME);
	}

	@Test
	public void injectWithNullRequestName() throws Exception {
		final BoomerangJsHtmlInjector injector = new BoomerangJsHtmlInjector();
		final Configuration configuration = mock(Configuration.class);
		final WebPlugin webPlugin = mock(WebPlugin.class);
		when(configuration.getConfig(WebPlugin.class)).thenReturn(webPlugin);
		final RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		when(requestMonitorPlugin.getTracer()).thenReturn(new com.uber.jaeger.Tracer.Builder(getClass().getSimpleName(), new NoopReporter(), new ConstSampler(true)).build());
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		injector.init(new HtmlInjector.InitArguments(configuration, new MockServletContext()));

		final RequestMonitor.RequestInformation requestInformation = mock(RequestMonitor.RequestInformation.class);
		final com.uber.jaeger.Span span = SpanUtils.getInternalSpan(
				new MonitoredHttpRequest(
						new MockHttpServletRequest("GET", "/index.html"),
						mock(StatusExposingByteCountingServletResponse.class), new MockFilterChain(), configuration)
						.createSpan());
		when(requestInformation.getInternalSpan()).thenReturn(span);

		final HtmlInjector.InjectArguments injectArguments = new HtmlInjector.InjectArguments(requestInformation);
		injector.injectHtml(injectArguments);
		assertEquals("<script src=\"/stagemonitor/public/static/rum/boomerang-56c823668fc.min.js\"></script>\n" +
				"<script>\n" +
				"   BOOMR.init({\n" +
				"      log: null\n" +
				"   });\n" +
				"   BOOMR.addVar(\"requestId\", \"" + StringUtils.toHexString(span.context().getSpanID()) + "\");\n" +
				"   BOOMR.addVar(\"requestName\", \"GET /index.html\");\n" +
				"   BOOMR.addVar(\"serverTime\", 0);\n" +
				"</script>", injectArguments.getContentToInjectBeforeClosingBody());
	}

}
