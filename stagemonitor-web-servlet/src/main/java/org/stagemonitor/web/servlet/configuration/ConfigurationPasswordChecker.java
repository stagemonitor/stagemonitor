package org.stagemonitor.web.servlet.configuration;

import org.stagemonitor.configuration.ConfigurationRegistry;

public class ConfigurationPasswordChecker {

	private final ConfigurationRegistry configurationRegistry;
	private final String updateConfigPasswordKey;

	public ConfigurationPasswordChecker(ConfigurationRegistry configurationRegistry, String updateConfigPasswordKey) {
		this.configurationRegistry = configurationRegistry;
		this.updateConfigPasswordKey = updateConfigPasswordKey;
	}

	/**
	 * Validates a password.
	 *
	 * @param password the provided password to validate
	 * @return <code>true</code>, if the password is correct, <code>false</code> otherwise
	 */
	public boolean isPasswordCorrect(String password) {
		final String actualPassword = configurationRegistry.getString(updateConfigPasswordKey);
		return "".equals(actualPassword) || actualPassword != null && actualPassword.equals(password);
	}

	/**
	 * Validates a password. If not valid, throws a {@link IllegalStateException}.
	 *
	 * @param password the provided password to validate
	 * @throws IllegalStateException if the password did not match
	 */
	public void assertPasswordCorrect(String password) {
		if (!isPasswordSet()) {
			throw new IllegalStateException("'" + updateConfigPasswordKey + "' is not set.");
		}

		if (!isPasswordCorrect(password)) {
			throw new IllegalStateException("Wrong password for '" + updateConfigPasswordKey + "'.");
		}
	}

	/**
	 * Returns <code>true</code>, if the password that is required to save
	 * settings is set (not <code>null</code>), <code>false</code> otherwise
	 *
	 * @return <code>true</code>, if the update configuration password is set, <code>false</code> otherwise
	 */
	public boolean isPasswordSet() {
		return configurationRegistry.getString(updateConfigPasswordKey) != null;
	}
}
