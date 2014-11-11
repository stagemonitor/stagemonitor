package org.stagemonitor.core.configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.configuration.converter.BooleanValueConverter;
import org.stagemonitor.core.configuration.converter.IntegerValueConverter;
import org.stagemonitor.core.configuration.converter.LongValueConverter;
import org.stagemonitor.core.configuration.converter.RegexListValueConverter;
import org.stagemonitor.core.configuration.converter.RegexMapValueConverter;
import org.stagemonitor.core.configuration.converter.StringValueConverter;
import org.stagemonitor.core.configuration.converter.StringsValueConverter;
import org.stagemonitor.core.configuration.converter.ValueConverter;
import org.stagemonitor.core.configuration.source.ConfigurationSource;

/**
 * Represents a configuration option
 *
 * @param <T> the type of the configuration value
 */
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
	private final String configurationCategory;
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

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link String}
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link String}
	 */
	public static  ConfigurationOptionBuilder<String> stringOption() {
		return new ConfigurationOptionBuilder<String>(STRING_VALUE_CONVERTER, String.class);
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link Boolean}
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link Boolean}
	 */
	public static  ConfigurationOptionBuilder<Boolean> booleanOption() {
		return new ConfigurationOptionBuilder<Boolean>(BOOLEAN_VALUE_CONVERTER, Boolean.class);
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link Integer}
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link Integer}
	 */
	public static  ConfigurationOptionBuilder<Integer> integerOption() {
		return new ConfigurationOptionBuilder<Integer>(INTEGER_VALUE_CONVERTER, Integer.class);
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link Long}
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link Long}
	 */
	public static ConfigurationOptionBuilder<Long> longOption() {
		return new ConfigurationOptionBuilder<Long>(LONG_VALUE_CONVERTER, Long.class);
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link List}&lt{@link String}>
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link List}&lt{@link String}>
	 */
	public static ConfigurationOptionBuilder<Collection<String>> stringsOption() {
		return new ConfigurationOptionBuilder<Collection<String>>(STRINGS_VALUE_CONVERTER, Collection.class)
				.defaultValue(Collections.<String>emptySet());
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link List}&lt{@link String}>
	 * and all Strings are converted to lower case.
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link List}&lt{@link String}>
	 */
	public static ConfigurationOptionBuilder<Collection<String>> lowerStringsOption() {
		return new ConfigurationOptionBuilder<Collection<String>>(LOWER_STRINGS_VALUE_CONVERTER, Collection.class)
				.defaultValue(Collections.<String>emptySet());
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link List}&lt{@link Pattern}>
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link List}&lt{@link Pattern}>
	 */
	public static ConfigurationOptionBuilder<List<Pattern>> regexListOption() {
		return new ConfigurationOptionBuilder<List<Pattern>>(PATTERNS_VALUE_CONVERTER, List.class)
				.defaultValue(Collections.<Pattern>emptyList());
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link Map}&lt{@link Pattern}, {@link String}>
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link Map}&lt{@link Pattern}, {@link String}>
	 */
	public static ConfigurationOptionBuilder<Map<Pattern, String>> regexMapOption() {
		return new ConfigurationOptionBuilder<Map<Pattern, String>>(REGEX_MAP_VALUE_CONVERTER, Map.class)
				.defaultValue(Collections.<Pattern, String>emptyMap());
	}

	private ConfigurationOption(boolean dynamic, String key, String label, String description,
								T defaultValue, String configurationCategory, ValueConverter<T> valueConverter,
								Class<? super T> valueType) {
		this.dynamic = dynamic;
		this.key = key;
		this.label = label;
		this.description = description;
		this.defaultValue = defaultValue;
		this.defaultValueAsString = valueConverter.toString(defaultValue);
		this.configurationCategory = configurationCategory;
		this.valueConverter = valueConverter;
		this.valueType = valueType;
	}

	/**
	 * Returns <code>true</code>, if the value can dynamically be set, <code>false</code> otherwise.
	 *
	 * @return <code>true</code>, if the value can dynamically be set, <code>false</code> otherwise.
	 */
	public boolean isDynamic() {
		return dynamic;
	}

	/**
	 * Returns the key of the configuration option that can for example be used in a properties file
	 *
	 * @return the config key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Returns the display name of this configuration option
	 *
	 * @return the display name of this configuration option
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Returns the description of the configuration option
	 *
	 * @return the description of the configuration option
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the default value in its string representation
	 *
	 * @return the default value as string
	 */
	public String getDefaultValueAsString() {
		return defaultValueAsString;
	}

	/**
	 * Returns the current in its string representation
	 *
	 * @return the current value as string
	 */
	public String getValueAsString() {
		return valueAsString;
	}

	/**
	 * Returns the current value
	 *
	 * @return the current value
	 */
	@JsonIgnore
	public T getValue() {
		return value;
	}

	void setConfigurationSources(List<ConfigurationSource> configurationSources) {
		this.configurationSources = configurationSources;
		loadValue();
	}

	/**
	 * Returns the name of the configuration source that provided the current value
	 *
	 * @return the name of the configuration source that provided the current value
	 */
	public String getNameOfCurrentConfigurationSource() {
		return nameOfCurrentConfigurationSource;
	}


	/**
	 * Returns the category name of this configuration option
	 *
	 * @return the category name of this configuration option
	 */
	public String getConfigurationCategory() {
		return configurationCategory;
	}

	/**
	 * Returns the simple type name of the value
	 *
	 * @return the simple type name of the value
	 */
	public String getValueType() {
		return valueType.getSimpleName();
	}

	/**
	 * If there was a error while trying to set value from a {@link ConfigurationSource}, this error message contains
	 * information about the error.
	 *
	 * @return a error message or null if there was no error
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	synchronized void reload() {
		// non-dynamic options can't be reloaded
		if (dynamic) {
			loadValue();
		}
	}

	private void loadValue() {
		String newValue = null;
		for (ConfigurationSource configurationSource : configurationSources) {
			newValue = configurationSource.getValue(key);
			nameOfCurrentConfigurationSource = configurationSource.getName();
			if (newValue != null) {
				break;
			}
		}
		if (newValue == null || !trySetValue(newValue)) {
			setToDefault();
		}
	}

	private boolean trySetValue(String newValue) {
		newValue = newValue.trim();
		if (hasChanges(newValue)) {
			this.valueAsString = newValue;
			try {
				value = valueConverter.convert(newValue);
				return true;
			} catch (IllegalArgumentException e) {
				errorMessage = "Error in " + nameOfCurrentConfigurationSource + ": " + e.getMessage();
				logger.warn(errorMessage + " Default value '" + defaultValueAsString + "' will be applied.", e);
				return false;
			}
		} else {
			return true;
		}
	}

	private void setToDefault() {
		valueAsString = defaultValueAsString;
		value = defaultValue;
		nameOfCurrentConfigurationSource = "Default Value";
	}

	private boolean hasChanges(String property) {
		return !property.equals(valueAsString);
	}

	/**
	 * Throws a {@link IllegalArgumentException} if the value is not valid
	 *
	 * @param value the configuration value as string
	 * @throws IllegalArgumentException if there was a error while converting the value
	 */
	public void assertValid(String value) throws IllegalArgumentException {
		valueConverter.convert(value);
	}

	public static class ConfigurationOptionBuilder<T> {
		private boolean dynamic = false;
		private String key;
		private String label;
		private String description;
		private T defaultValue;
		private String configurationCategory;
		private ValueConverter<T> valueConverter;
		private Class<? super T> valueType;

		private ConfigurationOptionBuilder(ValueConverter<T> valueConverter, Class<? super T> valueType) {
			this.valueConverter = valueConverter;
			this.valueType = valueType;
		}

		public ConfigurationOption<T> build() {
			return new ConfigurationOption<T>(dynamic, key, label, description, defaultValue, configurationCategory,
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

		public ConfigurationOptionBuilder<T> configurationCategory(String configurationCategory) {
			this.configurationCategory = configurationCategory;
			return this;
		}
	}
}
