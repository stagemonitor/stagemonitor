package org.stagemonitor.alerting.alerter;

import java.io.StringWriter;
import java.util.Collections;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.incident.Incident;

public class AlertTemplateProcessor {

	private final AlertingPlugin alertingPlugin;
	private final Configuration cfg;

	public AlertTemplateProcessor(AlertingPlugin alertingPlugin) {
		this.alertingPlugin = alertingPlugin;
		cfg = new Configuration(Configuration.VERSION_2_3_22);
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
	}

	public String processHtmlTemplate(Incident incident) {
		return processTemplate("alertsHtml.ftl", alertingPlugin.getHtmlAlertTemplate(), incident);
	}

	public String processPlainTextTemplate(Incident incident) {
		return processTemplate("alertsPlainText.ftl", alertingPlugin.getPlainTextAlertTemplate(), incident);
	}

	public String processShortDescriptionTemplate(Incident incident) {
		return processTemplate("alertsShortDescription.ftl", alertingPlugin.getShortDescriptionAlertTemplate(), incident);
	}

	private String processTemplate(String templateName, String templateString, Incident incident) {
		try {
			Template template = new Template(templateName, templateString, cfg);
			StringWriter out = new StringWriter(templateString.length());
			template.process(Collections.singletonMap("incident", incident), out);
			return out.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
