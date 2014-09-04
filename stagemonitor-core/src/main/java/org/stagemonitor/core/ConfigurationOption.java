package org.stagemonitor.core;

public class ConfigurationOption {

	private final boolean dynamic;
	private final String key;
	private final String label;
	private final String description;
	private final String defaultValue;
	private ConfigurationSource currentValueSource;

	public static ConfigurationOptionBuilder builder() {
		return new ConfigurationOptionBuilder();
	}

	private ConfigurationOption(boolean dynamic, String key, String label, String description, String defaultValue) {
		this.dynamic = dynamic;
		this.key = key;
		this.label = label;
		this.description = description;
		this.defaultValue = defaultValue;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	public String getKey() {
		return key;
	}

	public String getLabel() {
		return label;
	}

	public String getDescription() {
		return description;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public String getCurrentValue() {
		return currentValueSource.getValue(key);
	}

	public void setCurrentValueSource(ConfigurationSource currentValueSource) {
		this.currentValueSource = currentValueSource;
	}

	public static class ConfigurationOptionBuilder {
		private boolean dynamic = false;
		private String key;
		private String label;
		private String description;
		private String defaultValue;

		public ConfigurationOption build() {
			return new ConfigurationOption(dynamic, key, label, description, defaultValue);
		}

		public ConfigurationOptionBuilder dynamic(boolean dynamic) {
			this.dynamic = dynamic;
			return this;
		}

		public ConfigurationOptionBuilder key(String key) {
			this.key = key;
			return this;
		}

		public ConfigurationOptionBuilder label(String label) {
			this.label = label;
			return this;
		}

		public ConfigurationOptionBuilder description(String description) {
			this.description = description;
			return this;
		}

		public ConfigurationOptionBuilder defaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
			return this;
		}
	}
}
