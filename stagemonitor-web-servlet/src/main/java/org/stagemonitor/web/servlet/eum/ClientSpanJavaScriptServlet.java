package org.stagemonitor.web.servlet.eum;

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
	private static final String CACHE_CONTROL = "Cache-Control";
	private final String javaScript;
	private final String javaScriptEtag;

	public ClientSpanJavaScriptServlet() {
		List<ClientSpanExtensionSPI> clientSpanExtensionSPIS = Stagemonitor.getPlugin(ServletPlugin.class).getClientSpanExtenders();
		javaScript = buildJavaScript(clientSpanExtensionSPIS);
		javaScriptEtag = generateEtag(javaScript);
	}

	private String generateEtag(String javaScript) {
		return String.format("\"%d\"", javaScript.hashCode());
	}

	private String buildJavaScript(List<ClientSpanExtensionSPI> clientSpanExtensionSPIS) {
		StringBuilder javaScriptBuilder = new StringBuilder();
		for (ClientSpanExtensionSPI contributor : clientSpanExtensionSPIS) {
			javaScriptBuilder.append(wrapImmediateInvokedFunctionExpression(contributor.getClientTraceExtensionScript()));
		}
		return javaScriptBuilder.toString();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setHeader(ETAG, javaScriptEtag);
		resp.setHeader(CACHE_CONTROL, getCacheControlMaxAge());
		if (isInClientCache(req)) {
			resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
		} else {
			resp.setContentType(MIME_APPLICATION_JAVASCRIPT);
			resp.getWriter().write(javaScript);
		}
	}

	private boolean isInClientCache(HttpServletRequest req) {
		return javaScriptEtag.equals(req.getHeader(IF_NONE_MATCH));
	}

	private String wrapImmediateInvokedFunctionExpression(String toWrap) {
		return "(function() {" + toWrap + "})();";
	}

	private String getCacheControlMaxAge() {
		return MAX_AGE + String.valueOf(TimeUnit.MINUTES.toSeconds(5)); // TODO configurable?
	}
}
