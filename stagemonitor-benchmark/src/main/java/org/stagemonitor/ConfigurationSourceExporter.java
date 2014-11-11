package org.stagemonitor;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.JsonUtils;

public class ConfigurationSourceExporter {

	public static void main(String[] args) throws IOException {
		final String json = JsonUtils.toJson(new Configuration(StagemonitorPlugin.class).getConfigurationOptionsByCategory());
		FileUtils.writeStringToFile(new File(args[0]), json);
	}
}
