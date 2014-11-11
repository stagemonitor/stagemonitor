package org.stagemonitor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;

public class ConfigurationOptionsMarkdownExporter {

	public static void main(String[] args) throws IOException {
		final Map<String, List<ConfigurationOption<?>>> configurationOptionsByPlugin = new Configuration(StagemonitorPlugin.class).getConfigurationOptionsByCategory();

		StringBuilder markdown = new StringBuilder();
		for (Map.Entry<String, List<ConfigurationOption<?>>> entry : configurationOptionsByPlugin.entrySet()) {
			markdown.append("# ").append(entry.getKey()).append(" Plugin\n\n");
			for (ConfigurationOption<?> configurationOption : entry.getValue()) {
				markdown.append("## ").append(configurationOption.getLabel()).append("\n\n");
				markdown.append(configurationOption.getDescription()).append("\n\n");
				markdown.append("Key: `").append(configurationOption.getKey()).append("`\n\n");
				markdown.append("Default Value: ");
				markdown.append('`').append(configurationOption.getDefaultValueAsString()).append('`');
				markdown.append("\n\n");
			}
			markdown.append("***\n\n");
		}
		System.out.println(markdown);
	}
}
