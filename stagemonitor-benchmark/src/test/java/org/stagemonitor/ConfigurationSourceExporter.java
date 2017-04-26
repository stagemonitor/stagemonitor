package org.stagemonitor;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.util.JsonUtils;

public class ConfigurationSourceExporter {

	private ConfigurationSourceExporter() {
	}

	public static void main(String[] args) throws IOException {
		final String json = JsonUtils.toJson(new ConfigurationRegistry(StagemonitorPlugin.class).getConfigurationOptionsByCategory());
		System.out.println(json);
		File stagemonitorWidgetDevHtml = new File("stagemonitor-web/src/test/resources/stagemonitorWidgetDev.html");
		String content = FileUtils.readFileToString(stagemonitorWidgetDevHtml);
		content = content.replaceAll("configurationOptions .*", "configurationOptions = " + json.replace("$", "\\$").replace("\\\"", "\\\\\"") + ";");

		FileUtils.writeStringToFile(stagemonitorWidgetDevHtml, content);
	}
}
