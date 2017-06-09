package org.stagemonitor.web.servlet.eum;

import org.stagemonitor.web.servlet.ServletPlugin;
import org.stagemonitor.web.servlet.filter.HtmlInjector;

public class ClientSpanJavaScriptHtmlInjector extends HtmlInjector {

	private InitArguments initArguments;
	private ServletPlugin servletPlugin;

	@Override
	public void init(HtmlInjector.InitArguments initArguments) {
		this.initArguments = initArguments;
		this.servletPlugin = initArguments.getConfiguration().getConfig(ServletPlugin.class);
	}

	@Override
	public void injectHtml(InjectArguments injectArguments) {
		final String contextPath = initArguments.getServletContext().getContextPath();
		injectArguments.setContentToInjectBeforeClosingBody(
				"<script type='text/javascript'>\n" +
						"  (function(i,s,o,g,r,a,m) {\n" +
						"    i['EumObject']=r;\n" +
						"    i[r]=i[r] || function() {\n" +
						"      (i[r].q=i[r].q||[]).push(arguments)\n" +
						"    }\n" +
						"      ,i[r].l=1*new Date();\n" +
						"    a=s.createElement(o),\n" +
						"    m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)\n" +
						"  })(window,document,'script','" + contextPath + "/stagemonitor/public/eum.js','ineum');\n" +
						"  \n" +
						"  ineum('reportingUrl', '" + contextPath + "/stagemonitor/public/eum');\n" +
						//"  ineum('apiKey', 'someKey');\n" + // TODO
						"  ineum('meta', 'user', 'tom.mason@example.com');\n" + // TODO
						"  window.setTimeout(function() { throw null; }, 1500); \n" + // TODO
						"</script>\n");
	}

	@Override
	public boolean isActive(IsActiveArguments isActiveArguments) {
		return servletPlugin.isClientSpanCollectionEnabled();
	}
}
