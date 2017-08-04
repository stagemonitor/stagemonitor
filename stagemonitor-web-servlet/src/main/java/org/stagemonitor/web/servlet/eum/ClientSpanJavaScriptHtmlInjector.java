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
		final StringBuilder sb = new StringBuilder()
				.append("<script type='text/javascript'>\n")
				.append("  (function(i,s,o,g,r,a,m) {\n")
				.append("    i['EumObject']=r;\n")
				.append("    i[r]=i[r] || function() {\n")
				.append("      (i[r].q=i[r].q||[]).push(arguments)\n")
				.append("    }\n")
				.append("      ,i[r].l=1*new Date();\n")
				.append("    a=s.createElement(o),\n")
				.append("    m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)\n")
				.append("  })(window,document,'script','")
				.append(contextPath)
				.append("/stagemonitor/public/eum.js','ineum');\n")
				.append("  \n")
				.append("  ineum('reportingUrl', '")
				.append(contextPath)
				.append("/stagemonitor/public/eum');\n");
		for (ClientSpanExtension clientSpanExtension : servletPlugin.getClientSpanExtenders()) {
			sb.append(clientSpanExtension.getClientTraceExtensionScriptDynamicPart(injectArguments.getSpanWrapper()));
		}
		sb.append("</script>\n");
		injectArguments.setContentToInjectBeforeClosingHead(
				sb.toString());
	}

	@Override
	public boolean isActive(IsActiveArguments isActiveArguments) {
		return servletPlugin.isClientSpanCollectionEnabled() && servletPlugin.isClientSpanCollectionInjectionEnabled();
	}
}
