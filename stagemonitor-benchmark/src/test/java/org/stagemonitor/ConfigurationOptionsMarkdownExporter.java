package org.stagemonitor;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.util.StringUtils;

public class ConfigurationOptionsMarkdownExporter {

	private ConfigurationOptionsMarkdownExporter() {
	}

	public static void main(String[] args) throws IOException {
		StringBuilder markdown = new StringBuilder();
		final Map<String, List<ConfigurationOption<?>>> configurationOptionsByPlugin = new ConfigurationRegistry(StagemonitorPlugin.class).getConfigurationOptionsByCategory();

		MultiValueMap<String, ConfigurationOption<?>> configurationOptionsByTags = new LinkedMultiValueMap<>();
		configurationOptionsByPlugin.values()
				.stream()
				.flatMap(Collection::stream)
				.filter(opt -> !opt.getTags().isEmpty())
				.forEach(opt -> opt.getTags()
						.forEach(tag -> configurationOptionsByTags.add(tag, opt)));


		markdown.append("# Overview\n");
		markdown.append("## All Plugins\n");
		for (String plugin : configurationOptionsByPlugin.keySet()) {
			markdown.append("* ").append(linkToHeadline(plugin)).append('\n');
		}
		markdown.append("\n");
		markdown.append("## Available Tags\n");
		for (String tag : configurationOptionsByTags.keySet()) {
			markdown.append("`").append(tag).append("` ");
		}
		markdown.append("\n");

		markdown.append("# Options by Plugin\n");
		for (Map.Entry<String, List<ConfigurationOption<?>>> entry : configurationOptionsByPlugin.entrySet()) {
			final String pluginName = entry.getKey();
			markdown.append("* ").append(linkToHeadline(pluginName)).append('\n');
			for (ConfigurationOption<?> option : entry.getValue()) {
				markdown.append(" * ").append(linkToHeadline(option.getLabel())).append('\n');
			}
		}
		markdown.append("\n");

		markdown.append("# Options by Tag\n");
		markdown.append("\n");
		for (Map.Entry<String, List<ConfigurationOption<?>>> entry : configurationOptionsByTags.entrySet()) {
			markdown.append("* `").append(entry.getKey()).append("` \n");
			for (ConfigurationOption<?> option : entry.getValue()) {
				markdown.append(" * ").append(linkToHeadline(option.getLabel())).append('\n');
			}}
		markdown.append("\n");

		for (Map.Entry<String, List<ConfigurationOption<?>>> entry : configurationOptionsByPlugin.entrySet()) {
			markdown.append("# ").append(entry.getKey()).append("\n\n");
			for (ConfigurationOption<?> configurationOption : entry.getValue()) {
				markdown.append("## ").append(configurationOption.getLabel()).append("\n\n");
				markdown.append(configurationOption.getDescription()).append("\n\n");
				markdown.append("Key: `").append(configurationOption.getKey()).append("`\n\n");
				markdown.append("Default Value: ");
				markdown.append('`').append(configurationOption.getDefaultValueAsString()).append("`\n\n");
				if (!configurationOption.getTags().isEmpty()) {
					markdown.append("Tags: ");
					for (String tag : configurationOption.getTags()) {
						markdown.append('`').append(tag).append("` ");
					}
					markdown.append("\n\n");
				}
			}
			markdown.append("***\n\n");
		}
		System.out.println(markdown);
	}

	private static String linkToHeadline(String headline) {
		return "[" + headline + "](#" + StringUtils.slugify(headline) + ')';
	}
}
