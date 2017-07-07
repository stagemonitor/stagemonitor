package org.stagemonitor.web.servlet.filter.htmlinjector;

import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.tracing.MockTracer;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.util.IOUtils;
import org.stagemonitor.util.StringUtils;
import org.stagemonitor.web.servlet.ServletPlugin;
import org.stagemonitor.web.servlet.filter.HtmlInjector;
import org.stagemonitor.web.servlet.rum.BoomerangJsHtmlInjector;

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
		final ConfigurationRegistry configuration = mock(ConfigurationRegistry.class);
		final ServletPlugin servletPlugin = mock(ServletPlugin.class);
		when(configuration.getConfig(ServletPlugin.class)).thenReturn(servletPlugin);
		final TracingPlugin tracingPlugin = mock(TracingPlugin.class);
		when(tracingPlugin.getTracer()).thenReturn(new MockTracer());
		when(configuration.getConfig(TracingPlugin.class)).thenReturn(tracingPlugin);
		injector.init(new HtmlInjector.InitArguments(configuration, new MockServletContext()));

		final SpanWrapper span = mock(SpanWrapper.class);
		when(span.getOperationName()).thenReturn("GET /index.html");

		final HtmlInjector.InjectArguments injectArguments = new HtmlInjector.InjectArguments(span);
		injector.injectHtml(injectArguments);
		assertEquals("<script src=\"/stagemonitor/public/static/rum/boomerang-56c823668fc.min.js\"></script>\n" +
				"<script>\n" +
				"   BOOMR.init({\n" +
				"      log: null\n" +
				"   });\n" +
				"   BOOMR.addVar(\"requestName\", \"GET /index.html\");\n" +
				"   BOOMR.addVar(\"serverTime\", 0.0);\n" +
				"</script>", injectArguments.getContentToInjectBeforeClosingBody());
	}

}
