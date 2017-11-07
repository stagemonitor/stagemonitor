package org.stagemonitor.web.servlet.eum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.web.servlet.ServletPlugin;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ClientSpanJavaScriptServlet extends HttpServlet {

	private static final String MIME_APPLICATION_JAVASCRIPT = "application/javascript";
	private static final String ETAG = "etag";
	private static final String IF_NONE_MATCH = "if-none-match";
	private static final String MAX_AGE = "max-age=";
	private static final String NO_CACHE = "no-cache";
	static final String CACHE_CONTROL = "Cache-Control";
	private static final Logger log = LoggerFactory.getLogger(ClientSpanJavaScriptServlet.class);

	private final ServletPlugin servletPlugin;
	private String javaScript;
	private String javaScriptEtag;

	public ClientSpanJavaScriptServlet() {
		this(Stagemonitor.getPlugin(ServletPlugin.class));
	}

	public ClientSpanJavaScriptServlet(ServletPlugin servletPlugin) {
		this.servletPlugin = servletPlugin;
		rebuildJavaScriptAndEtag();
	}

	public void rebuildJavaScriptAndEtag() {
		List<ClientSpanExtension> clientSpanExtensions = Stagemonitor.getPlugin(ServletPlugin.class).getClientSpanExtenders();
		javaScript = concatenateJavaScript(clientSpanExtensions);
		javaScriptEtag = generateEtag(javaScript);
		log.info("built new end user monitoring JavaScript, size={} bytes, etag={}", javaScript.length(), javaScriptEtag);
	}

	private String generateEtag(String javaScript) {
		return String.format("\"%x\"", javaScript.hashCode());
	}

	private String concatenateJavaScript(List<ClientSpanExtension> clientSpanExtensions) {
		StringBuilder javaScriptBuilder = new StringBuilder();
		for (ClientSpanExtension contributor : clientSpanExtensions) {
			javaScriptBuilder.append(wrapImmediateInvokedFunctionExpression(contributor.getClientTraceExtensionScriptStaticPart()));
		}
		return javaScriptBuilder.toString();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (servletPlugin.isClientSpanCollectionEnabled()) {
			resp.setHeader(ETAG, javaScriptEtag);
			resp.setHeader(CACHE_CONTROL, getCacheControlMaxAge());
			if (isInClientCache(req)) {
				resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			} else {
				resp.setContentType(MIME_APPLICATION_JAVASCRIPT);
				resp.getWriter().write(javaScript);
			}
		} else {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	private boolean isInClientCache(HttpServletRequest req) {
		return javaScriptEtag.equals(req.getHeader(IF_NONE_MATCH));
	}

	private String wrapImmediateInvokedFunctionExpression(String toWrap) {
		return "(function() {" + toWrap + "})();";
	}

	private String getCacheControlMaxAge() {
		int cachingDurationInMinutes = servletPlugin.getClientSpanScriptCacheDuration();
		if (cachingDurationInMinutes <= 0) {
			return NO_CACHE;
		} else {
			return MAX_AGE + String.valueOf(TimeUnit.MINUTES.toSeconds(cachingDurationInMinutes));
		}
	}
}
