package org.stagemonitor.web.monitor.filter.htmlinjector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Collections;

import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.IOUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.HttpRequestTrace;
import org.stagemonitor.web.monitor.filter.HtmlInjector;
import org.stagemonitor.web.monitor.rum.BoomerangJsHtmlInjector;

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
		injector.init(new HtmlInjector.InitArguments(configuration, new MockServletContext()));

		final RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation = mock(RequestMonitor.RequestInformation.class);
		final HttpRequestTrace requestTrace = new HttpRequestTrace("1", "/index.html", Collections.emptyMap(), "GET", null, true);
		when(requestInformation.getRequestTrace()).thenReturn(requestTrace);

		final HtmlInjector.InjectArguments injectArguments = new HtmlInjector.InjectArguments(requestInformation);
		injector.injectHtml(injectArguments);
		assertEquals("<script src=\"/stagemonitor/public/static/rum/boomerang-56c823668fc.min.js\"></script>\n" +
				"<script>\n" +
				"   BOOMR.init({\n" +
				"      log: null\n" +
				"   });\n" +
				"   BOOMR.addVar(\"requestId\", \"1\");\n" +
				"   BOOMR.addVar(\"requestName\", \"null\");\n" +
				"   BOOMR.addVar(\"serverTime\", 0);\n" +
				"</script>", injectArguments.getContentToInjectBeforeClosingBody());
	}

}
