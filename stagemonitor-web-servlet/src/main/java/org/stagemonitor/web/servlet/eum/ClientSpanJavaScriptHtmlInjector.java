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
		injectArguments.setContentToInjectBeforeClosingHead(
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
						//"  ineum('meta', 'user', 'tom.mason@example.com');\n" + // example for setting some metadata
						"</script>\n");
	}

	@Override
	public boolean isActive(IsActiveArguments isActiveArguments) {
		return servletPlugin.isClientSpanCollectionEnabled() && servletPlugin.isClientSpanCollectionInjectionEnabled();
	}
}
