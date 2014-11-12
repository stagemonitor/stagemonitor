package org.stagemonitor.core.configuration.source;

import java.util.Map;

/**
 * This configuration source get its values from the operating system's environment variables.
 * <p/>
 * Because of the naming restrictions/conventions of environment variables, all dots ('.') are replaced with underscores
 * and all letters are converted to upper case.
 * <p/>
 * Example: To set the configuration key <code>stagemonitor.active</code>, the environment variable has to be
 * <code>STAGEMONITOR_ACTIVE</code>
 */
public class EnvironmentVariableConfigurationSource extends AbstractConfigurationSource {

	private Map<String, String> env;

	public EnvironmentVariableConfigurationSource() {
		reload();
	}

	public EnvironmentVariableConfigurationSource(Map<String, String> env) {
		this.env = env;
	}

	/**
	 * Returns the configuration value from the the operating system's environment variables.
	 * <p/>
	 * Because of the naming restrictions/conventions of environment variables, all dots ('.') are replaced with underscores
	 * and all letters are converted to upper case.
	 * <p/>
	 * Example: To set the configuration key <code>stagemonitor.active</code>, the environment variable has to be
	 * <code>STAGEMONITOR_ACTIVE</code>
	 * 
	 * @param key the property key
	 * @return the value
	 */
	@Override
	public String getValue(String key) {
		return env.get(key.replace('.', '_').toUpperCase());
	}

	@Override
	public String getName() {
		return "Environment Variables";
	}

	@Override
	public void reload() {
		this.env = System.getenv();
	}
}
