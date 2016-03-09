package org.stagemonitor.os;

import org.stagemonitor.core.StagemonitorConfigurationSourceInitializer;
import org.stagemonitor.core.configuration.source.ConfigurationSource;
import org.stagemonitor.core.configuration.source.SimpleSource;

public class OsConfigurationSourceInitializer extends StagemonitorConfigurationSourceInitializer {

	private static ConfigurationSource argsConfigurationSource;

	@Override
	public void modifyConfigurationSources(ModifyArguments modifyArguments) {
		if (argsConfigurationSource != null) {
			modifyArguments.addConfigurationSourceAsFirst(argsConfigurationSource);
		}
	}

	static void addConfigurationSource(String[] args) {
		final SimpleSource source = getConfiguration(args);
		argsConfigurationSource = source;
	}

	public static SimpleSource getConfiguration(String[] args) {
		final SimpleSource source = new SimpleSource("Process Arguments");
		for (String arg : args) {
			if (!arg.matches("(.+)=(.+)")) {
				throw new IllegalArgumentException("Illegal argument '" + arg +
						"'. Arguments must be in form '<config-key>=<config-value>'");
			}
			final String[] split = arg.split("=");
			source.add(split[0], split[1]);
		}
		return source;
	}
}