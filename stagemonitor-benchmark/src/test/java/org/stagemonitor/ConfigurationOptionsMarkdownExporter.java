package org.stagemonitor;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.util.StringUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class ConfigurationOptionsMarkdownExporter {

	private ConfigurationOptionsMarkdownExporter() {
	}

	public static void main(String[] args) throws IOException {
		final ConfigurationRegistry configurationRegistry = new ConfigurationRegistry(StagemonitorPlugin.class);
		System.out.println(getMarkdown(configurationRegistry));
	}

	private static String getMarkdown(ConfigurationRegistry configurationRegistry) {
		StringBuilder markdown = new StringBuilder();
		final Map<String, List<ConfigurationOption<?>>> configurationOptionsByPlugin = new TreeMap<>(configurationRegistry.getConfigurationOptionsByCategory());

		MultiValueMap<String, ConfigurationOption<?>> configurationOptionsByTags = new LinkedMultiValueMap<>();
		configurationOptionsByPlugin.values()
				.stream()
				.flatMap(Collection::stream)
				.filter(opt -> !opt.getTags().isEmpty())
				.forEach(opt -> opt.getTags()
						.forEach(tag -> configurationOptionsByTags.add(tag, opt)));


		markdown.append("# Overview\n");
		markdown.append("## All Plugins\n");
		for (String plugin : new TreeSet<>(configurationOptionsByPlugin.keySet())) {
			markdown.append("* ").append(linkToHeadline(plugin)).append('\n');
		}
		markdown.append("\n");
		markdown.append("## Available Tags\n");
		for (String tag : new TreeSet<>(configurationOptionsByTags.keySet())) {
			markdown.append("`").append(tag).append("` ");
		}
		markdown.append("\n");

		markdown.append("# Options by Plugin\n");
		for (Map.Entry<String, List<ConfigurationOption<?>>> entry : configurationOptionsByPlugin.entrySet()) {
			final String pluginName = entry.getKey();
			markdown.append("* ").append(linkToHeadline(pluginName)).append('\n');
			for (ConfigurationOption<?> option : entry.getValue()) {
				markdown.append("  * ").append(linkToHeadline(option.getLabel())).append('\n');
			}
		}
		markdown.append("\n");

		markdown.append("# Options by Tag\n");
		markdown.append("\n");
		for (Map.Entry<String, List<ConfigurationOption<?>>> entry : configurationOptionsByTags.entrySet()) {
			markdown.append("## `").append(entry.getKey()).append("` \n");
			for (ConfigurationOption<?> option : entry.getValue()) {
				markdown.append(" * ").append(linkToHeadline(option.getLabel())).append('\n');
			}}
		markdown.append("\n");

		for (Map.Entry<String, List<ConfigurationOption<?>>> entry : configurationOptionsByPlugin.entrySet()) {
			markdown.append("# ").append(entry.getKey()).append("\n\n");
			for (ConfigurationOption<?> configurationOption : entry.getValue()) {
				markdown.append("## ").append(configurationOption.getLabel()).append("\n\n");
				if (configurationOption.getDescription() != null) {
					markdown.append(configurationOption.getDescription()).append("\n\n");
				}
				markdown.append("Key: `").append(configurationOption.getKey()).append("`\n\n");
				markdown.append("Default Value: ");
				final String defaultValue = configurationOption.getDefaultValueAsString();
				if (defaultValue == null) {
					markdown.append("`null`\n\n");
				} else if (!defaultValue.contains("\n")) {
					markdown.append('`').append(defaultValue).append("`\n\n");
				} else {
					markdown.append("\n\n```\n").append(defaultValue).append("\n```\n\n");
				}
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
		return markdown.toString();
	}

	private static String linkToHeadline(String headline) {
		return "[" + headline + "](#" + StringUtils.slugify(headline) + ')';
	}
}
