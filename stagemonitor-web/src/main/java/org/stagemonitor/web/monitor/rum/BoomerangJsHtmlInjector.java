package org.stagemonitor.web.monitor.rum;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.HttpRequestTrace;
import org.stagemonitor.web.monitor.filter.HtmlInjector;

public class BoomerangJsHtmlInjector implements HtmlInjector {

	public static final String BOOMERANG_FILENAME = "boomerang-56c823668fc.min.js";
	private WebPlugin webPlugin;
	private String boomerangTemplate;
	private Configuration configuration;

	@Override
	public void init(Configuration configuration, ServletContext servletContext) {
		this.configuration = configuration;
		this.webPlugin = configuration.getConfig(WebPlugin.class);
		this.boomerangTemplate = buildBoomerangTemplate(servletContext.getContextPath());
	}

	private String buildBoomerangTemplate(String contextPath) {
		String beaconUrl = webPlugin.isRealUserMonitoringEnabled() ?
				"      beacon_url: " + "'" + contextPath + "/stagemonitor/public/rum'" + ",\n" : "";
		return "<script src=\"" + contextPath + "/stagemonitor/public/static/rum/" + BOOMERANG_FILENAME + "\"></script>\n" +
				"<script>\n" +
				"   BOOMR.init({\n" +
				beaconUrl +
				"      log: null\n" +
				"   });\n" +
				"   BOOMR.addVar(\"requestId\", \"${requestId}\");\n" +
				"   BOOMR.addVar(\"requestName\", \"${requestName}\");\n" +
				"   BOOMR.addVar(\"serverTime\", ${serverTime});\n" +
				"</script>";
	}

	@Override
	public boolean isActive(HttpServletRequest httpServletRequest) {
		// if widget is enabled, inject as well to render page load time statistics in widget
		// metrics won't be collected in this case, because the beacon_url is then set to null
		return webPlugin.isRealUserMonitoringEnabled() || webPlugin.isWidgetAndStagemonitorEndpointsAllowed(httpServletRequest, configuration);
	}

	@Override
	public String getContentToInjectBeforeClosingBody(RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation) {
		if (requestInformation == null || requestInformation.getRequestTrace() == null) {
			return "";
		}
		final HttpRequestTrace requestTrace = requestInformation.getRequestTrace();
		return boomerangTemplate.replace("${requestId}", String.valueOf(requestTrace.getId()))
				.replace("${requestName}", requestTrace.getName())
				.replace("${serverTime}", Long.toString(requestTrace.getExecutionTime()));
	}

}
