package org.stagemonitor.core.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.converter.BooleanValueConverter;
import org.stagemonitor.core.converter.IntegerValueConverter;
import org.stagemonitor.core.converter.LongValueConverter;
import org.stagemonitor.core.converter.RegexListValueConverter;
import org.stagemonitor.core.converter.RegexMapValueConverter;
import org.stagemonitor.core.converter.StringValueConverter;
import org.stagemonitor.core.converter.StringsValueConverter;
import org.stagemonitor.core.converter.ValueConverter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ConfigurationOption<T> {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	public static final ValueConverter<String> STRING_VALUE_CONVERTER = new StringValueConverter();
	public static final ValueConverter<Collection<String>> STRINGS_VALUE_CONVERTER = new StringsValueConverter();
	public static final ValueConverter<Collection<String>> LOWER_STRINGS_VALUE_CONVERTER = new StringsValueConverter(true);
	public static final ValueConverter<List<Pattern>> PATTERNS_VALUE_CONVERTER = new RegexListValueConverter();
	public static final ValueConverter<Map<Pattern, String>> REGEX_MAP_VALUE_CONVERTER = new RegexMapValueConverter();
	public static final ValueConverter<Boolean> BOOLEAN_VALUE_CONVERTER = new BooleanValueConverter();
	public static final ValueConverter<Integer> INTEGER_VALUE_CONVERTER = new IntegerValueConverter();
	public static final ValueConverter<Long> LONG_VALUE_CONVERTER = new LongValueConverter();

	private final boolean dynamic;
	private final String key;
	private final String label;
	private final String description;
	private final T defaultValue;
	private final String defaultValueAsString;
	private final String pluginName;
	private final ValueConverter<T> valueConverter;
	private final Class<? super T> valueType;
	private String valueAsString;
	private T value;
	private List<ConfigurationSource> configurationSources;
	private String nameOfCurrentConfigurationSource;
	private String errorMessage;

	public static <T> ConfigurationOptionBuilder<T> builder(ValueConverter<T> valueConverter, Class<T> valueType) {
		return new ConfigurationOptionBuilder<T>(valueConverter, valueType);
	}

	public static  ConfigurationOptionBuilder<String> stringOption() {
		return new ConfigurationOptionBuilder<String>(STRING_VALUE_CONVERTER, String.class);
	}
	public static  ConfigurationOptionBuilder<Boolean> booleanOption() {
		return new ConfigurationOptionBuilder<Boolean>(BOOLEAN_VALUE_CONVERTER, Boolean.class);
	}

	public static  ConfigurationOptionBuilder<Integer> integerOption() {
		return new ConfigurationOptionBuilder<Integer>(INTEGER_VALUE_CONVERTER, Integer.class);
	}

	public static ConfigurationOptionBuilder<Long> longOption() {
		return new ConfigurationOptionBuilder<Long>(LONG_VALUE_CONVERTER, Long.class);
	}

	public static ConfigurationOptionBuilder<Collection<String>> stringsOption() {
		return new ConfigurationOptionBuilder<Collection<String>>(STRINGS_VALUE_CONVERTER, Collection.class)
				.defaultValue(Collections.<String>emptySet());
	}

	public static ConfigurationOptionBuilder<Collection<String>> lowerStringsOption() {
		return new ConfigurationOptionBuilder<Collection<String>>(LOWER_STRINGS_VALUE_CONVERTER, Collection.class)
				.defaultValue(Collections.<String>emptySet());
	}

	public static ConfigurationOptionBuilder<List<Pattern>> regexListOption() {
		return new ConfigurationOptionBuilder<List<Pattern>>(PATTERNS_VALUE_CONVERTER, List.class)
				.defaultValue(Collections.<Pattern>emptyList());
	}
	public static ConfigurationOptionBuilder<Map<Pattern, String>> regexMapOption() {
		return new ConfigurationOptionBuilder<Map<Pattern, String>>(REGEX_MAP_VALUE_CONVERTER, Map.class)
				.defaultValue(Collections.<Pattern, String>emptyMap());
	}

	private ConfigurationOption(boolean dynamic, String key, String label, String description,
								T defaultValue, String pluginName, ValueConverter<T> valueConverter,
								Class<? super T> valueType) {
		this.dynamic = dynamic;
		this.key = key;
		this.label = label;
		this.description = description;
		this.defaultValue = defaultValue;
		this.defaultValueAsString = valueConverter.toString(defaultValue);
		this.pluginName = pluginName;
		this.valueConverter = valueConverter;
		this.valueType = valueType;
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

	public String getDefaultValueAsString() {
		return defaultValueAsString;
	}

	public String getValueAsString() {
		return valueAsString;
	}

	public T getValue() {
		return value;
	}

	void setConfigurationSources(List<ConfigurationSource> configurationSources) {
		this.configurationSources = configurationSources;
	}

	public String getNameOfCurrentConfigurationSource() {
		return nameOfCurrentConfigurationSource;
	}

	public String getPluginName() {
		return pluginName;
	}

	public String getValueType() {
		return valueType.getSimpleName();
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	synchronized void reload() {
		String property = null;
		for (ConfigurationSource configurationSource : configurationSources) {
			property = configurationSource.getValue(key);
			nameOfCurrentConfigurationSource = configurationSource.getName();
			if (property != null) {
				break;
			}
		}
		if (property != null) {
			if (trySetValue(property)) {
				return;
			}
		}
		value = defaultValue;
		nameOfCurrentConfigurationSource = "Default Value";
	}

	private boolean trySetValue(String property) {
		property = property.trim();
		this.valueAsString = property;
		try {
			value = valueConverter.convert(property);
			return true;
		} catch (IllegalArgumentException e) {
			errorMessage = e.getMessage();
			logger.warn(e.getMessage() + " Default value '" + defaultValueAsString + "' will now be applied.", e);
			return false;
		}
	}

	private void handleError(IllegalArgumentException e) {
		errorMessage = e.getMessage();
		logger.warn(e.getMessage() + " Default value '" + defaultValueAsString + "' will now be applied.", e);
	}

	public static class ConfigurationOptionBuilder<T> {
		private boolean dynamic = false;
		private String key;
		private String label;
		private String description;
		private T defaultValue;
		private String pluginName;
		private ValueConverter<T> valueConverter;
		private Class<? super T> valueType;

		private ConfigurationOptionBuilder(ValueConverter<T> valueConverter, Class<? super T> valueType) {
			this.valueConverter = valueConverter;
			this.valueType = valueType;
		}

		public ConfigurationOption<T> build() {
			return new ConfigurationOption<T>(dynamic, key, label, description, defaultValue, pluginName,
					valueConverter, valueType);
		}

		public ConfigurationOptionBuilder<T> dynamic(boolean dynamic) {
			this.dynamic = dynamic;
			return this;
		}

		public ConfigurationOptionBuilder<T> key(String key) {
			this.key = key;
			return this;
		}

		public ConfigurationOptionBuilder<T> label(String label) {
			this.label = label;
			return this;
		}

		public ConfigurationOptionBuilder<T> description(String description) {
			this.description = description;
			return this;
		}

		public ConfigurationOptionBuilder<T> defaultValue(T defaultValue) {
			this.defaultValue = defaultValue;
			return this;
		}

		public ConfigurationOptionBuilder<T> pluginName(String pluginName) {
			this.pluginName = pluginName;
			return this;
		}
	}
}
