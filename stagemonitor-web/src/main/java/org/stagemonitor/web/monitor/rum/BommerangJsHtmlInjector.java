package org.stagemonitor.web.monitor.rum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.HttpRequestTrace;
import org.stagemonitor.web.monitor.filter.HtmlInjector;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class BommerangJsHtmlInjector implements HtmlInjector {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String BOOMERANG_FILENAME = "boomerang-56c823668fc.min.js";
	private final WebPlugin webPlugin;
	private String boomerangTemplate;

	public BommerangJsHtmlInjector(WebPlugin webPlugin, String contextPath) {
		this.webPlugin = webPlugin;
		this.boomerangTemplate = buildBoomerangTemplate(contextPath);
	}

	private String buildBoomerangTemplate(String contextPath) {
		return "<script src=\"" + contextPath + "/stagemonitor/static/rum/" + BOOMERANG_FILENAME + "\"></script>\n" +
				"<script>\n" +
				"   BOOMR.init({\n" +
				"      beacon_url: \"" + contextPath + "/stagemonitor/rum\",\n" +
				"      log: null\n" +
				"   });\n" +
				"   BOOMR.addVar(\"requestId\", \"${requestId}\");\n" +
				"   BOOMR.addVar(\"requestName\", \"${requestName}\");\n" +
				"   BOOMR.addVar(\"serverTime\", ${serverTime});\n" +
				"</script>";
	}

	@Override
	public boolean isActive() {
		return webPlugin.isRealUserMonitoringEnabled();
	}

	@Override
	public String build(RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation) {
		final HttpRequestTrace requestTrace = requestInformation.getRequestTrace();
		try {
			return boomerangTemplate.replace("${requestId}", String.valueOf(requestTrace.getId()))
					.replace("${requestName}", URLEncoder.encode(requestTrace.getName(), "UTF-8"))
					.replace("${serverTime}", Long.toString(requestTrace.getExecutionTime()));
		} catch (UnsupportedEncodingException e) {
			logger.warn(e.getMessage(), e);
			return "";
		}
	}

}
