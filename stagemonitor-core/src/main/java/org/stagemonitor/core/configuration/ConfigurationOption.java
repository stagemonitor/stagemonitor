package org.stagemonitor.core.configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.configuration.converter.BooleanValueConverter;
import org.stagemonitor.core.configuration.converter.DoubleValueConverter;
import org.stagemonitor.core.configuration.converter.EnumValueConverter;
import org.stagemonitor.core.configuration.converter.IntegerValueConverter;
import org.stagemonitor.core.configuration.converter.JsonValueConverter;
import org.stagemonitor.core.configuration.converter.LongValueConverter;
import org.stagemonitor.core.configuration.converter.MapValueConverter;
import org.stagemonitor.core.configuration.converter.RegexValueConverter;
import org.stagemonitor.core.configuration.converter.SetValueConverter;
import org.stagemonitor.core.configuration.converter.StringValueConverter;
import org.stagemonitor.core.configuration.converter.ValueConverter;
import org.stagemonitor.core.configuration.source.ConfigurationSource;

/**
 * Represents a configuration option
 *
 * @param <T> the type of the configuration value
 */
public class ConfigurationOption<T> {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final boolean dynamic;
	private final boolean sensitive;
	private final String key;
	private final String label;
	private final String description;
	private final T defaultValue;
	private final List<String> tags;
	private final String defaultValueAsString;
	private final String configurationCategory;
	private final ValueConverter<T> valueConverter;
	private final Class<? super T> valueType;
	private String valueAsString;
	private T value;
	private List<ConfigurationSource> configurationSources;
	private String nameOfCurrentConfigurationSource;
	private String errorMessage;
	private Configuration configuration;

	public static <T> ConfigurationOptionBuilder<T> builder(ValueConverter<T> valueConverter, Class<? super T> valueType) {
		return new ConfigurationOptionBuilder<T>(valueConverter, valueType);
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link String}
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link String}
	 */
	public static  ConfigurationOptionBuilder<String> stringOption() {
		return new ConfigurationOptionBuilder<String>(StringValueConverter.INSTANCE, String.class);
	}

	public static <T> ConfigurationOptionBuilder<T> jsonOption(TypeReference<T> typeReference, Class<? super T> clazz) {
		return new ConfigurationOptionBuilder<T>(new JsonValueConverter<T>(typeReference), clazz);
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link Boolean}
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link Boolean}
	 */
	public static  ConfigurationOptionBuilder<Boolean> booleanOption() {
		return new ConfigurationOptionBuilder<Boolean>(BooleanValueConverter.INSTANCE, Boolean.class);
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link Integer}
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link Integer}
	 */
	public static  ConfigurationOptionBuilder<Integer> integerOption() {
		return new ConfigurationOptionBuilder<Integer>(IntegerValueConverter.INSTANCE, Integer.class);
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link Long}
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link Long}
	 */
	public static ConfigurationOptionBuilder<Long> longOption() {
		return new ConfigurationOptionBuilder<Long>(LongValueConverter.INSTANCE, Long.class);
	}
	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link Double}
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link Double}
	 */
	public static ConfigurationOptionBuilder<Double> doubleOption() {
		return new ConfigurationOptionBuilder<Double>(DoubleValueConverter.INSTANCE, Double.class);
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link List}&lt{@link String}>
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link List}&lt{@link String}>
	 */
	public static ConfigurationOptionBuilder<Collection<String>> stringsOption() {
		return new ConfigurationOptionBuilder<Collection<String>>(SetValueConverter.STRINGS_VALUE_CONVERTER, Collection.class)
				.defaultValue(Collections.<String>emptySet());
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link List}&lt{@link String}>
	 * and all Strings are converted to lower case.
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link List}&lt{@link String}>
	 */
	public static ConfigurationOptionBuilder<Collection<String>> lowerStringsOption() {
		return new ConfigurationOptionBuilder<Collection<String>>(SetValueConverter.LOWER_STRINGS_VALUE_CONVERTER, Collection.class)
				.defaultValue(Collections.<String>emptySet());
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link Set}&lt{@link Integer}>
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link Set}&lt{@link Integer}>
	 */
	public static ConfigurationOption.ConfigurationOptionBuilder<Collection<Integer>> integersOption() {
		return ConfigurationOption.builder(SetValueConverter.INTEGERS, Collection.class);
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link List}&lt{@link Pattern}>
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link List}&lt{@link Pattern}>
	 */
	public static ConfigurationOptionBuilder<Collection<Pattern>> regexListOption() {
		return new ConfigurationOptionBuilder<Collection<Pattern>>(new SetValueConverter<Pattern>(RegexValueConverter.INSTANCE), Collection.class)
				.defaultValue(Collections.<Pattern>emptySet());
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link Map}&lt{@link Pattern}, {@link String}>
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link Map}&lt{@link Pattern}, {@link String}>
	 */
	public static ConfigurationOptionBuilder<Map<Pattern, String>> regexMapOption() {
		return new ConfigurationOptionBuilder<Map<Pattern, String>>(MapValueConverter.REGEX_MAP_VALUE_CONVERTER, Map.class)
				.defaultValue(Collections.<Pattern, String>emptyMap());
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link Map}
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link Map}
	 */
	public static <K, V> ConfigurationOptionBuilder<Map<K, V>> mapOption(ValueConverter<K> keyConverter, ValueConverter<V> valueConverter) {
		return new ConfigurationOptionBuilder<Map<K, V>>(new MapValueConverter<K, V>(keyConverter, valueConverter), Map.class)
				.defaultValue(Collections.<K, V>emptyMap());
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is an {@link Enum}
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is an {@link Enum}
	 */
	public static <T extends Enum<T>> ConfigurationOptionBuilder<T> enumOption(Class<T> clazz) {
		return new ConfigurationOptionBuilder<T>(new EnumValueConverter<T>(clazz), clazz);
	}

	private ConfigurationOption(boolean dynamic, boolean sensitive, String key, String label, String description,
								T defaultValue, String configurationCategory, ValueConverter<T> valueConverter,
								Class<? super T> valueType, List<String> tags) {
		this.dynamic = dynamic;
		this.key = key;
		this.label = label;
		this.description = description;
		this.defaultValue = defaultValue;
		this.tags = tags;
		this.defaultValueAsString = valueConverter.toString(defaultValue);
		this.configurationCategory = configurationCategory;
		this.valueConverter = valueConverter;
		this.valueType = valueType;
		this.sensitive = sensitive;
		setToDefault();
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
	 * Returns <code>true</code>, if the value is sensitive, <code>false</code> otherwise.
	 * If a value has sensitive content (e.g. password), it should be rendered
	 * as an input of type="password", rather then as type="text".
	 *
	 * @return Returns <code>true</code>, if the value is sensitive, <code>false</code> otherwise.
	 */
	public boolean isSensitive() {
		return sensitive;
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

	/**
	 * Returns the current value
	 *
	 * @return the current value
	 */
	@JsonIgnore
	public T get() {
		return value;
	}

	void setConfigurationSources(List<ConfigurationSource> configurationSources) {
		this.configurationSources = configurationSources;
		loadValue();
	}

	void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
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
	 * Returns the tags associated with this configuration option
	 *
	 * @return the tags associated with this configuration option
	 */
	public List<String> getTags() {
		return Collections.unmodifiableList(tags);
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

	synchronized void reload(boolean reloadNonDynamicValues) {
		if (dynamic || reloadNonDynamicValues) {
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
				errorMessage = null;
				return true;
			} catch (IllegalArgumentException e) {
				errorMessage = "Error in " + nameOfCurrentConfigurationSource + ": " + e.getMessage();
				logger.warn(errorMessage + " Default value '" + defaultValueAsString + "' for '" + key + "' will be applied.");
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

	/**
	 * Updates the existing value with a new one
	 *
	 * @param newValue                the new value
	 * @param configurationSourceName the name of the configuration source that the value should be saved to
	 * @throws IOException                   if there was an error saving the key to the source
	 * @throws IllegalArgumentException      if there was a error processing the configuration key or value or the
	 *                                       configurationSourceName did not match any of the available configuration
	 *                                       sources
	 * @throws UnsupportedOperationException if saving values is not possible with this configuration source
	 */
	public void update(T newValue, String configurationSourceName) throws IOException {
		final String newValueAsString = valueConverter.toString(newValue);
		configuration.save(key, newValueAsString, configurationSourceName);
	}

	public static class ConfigurationOptionBuilder<T> {
		private boolean dynamic = false;
		private boolean sensitive = false;
		private String key;
		private String label;
		private String description;
		private T defaultValue;
		private String configurationCategory;
		private ValueConverter<T> valueConverter;
		private Class<? super T> valueType;
		private String[] tags = new String[0];

		private ConfigurationOptionBuilder(ValueConverter<T> valueConverter, Class<? super T> valueType) {
			this.valueConverter = valueConverter;
			this.valueType = valueType;
		}

		public ConfigurationOption<T> build() {
			return new ConfigurationOption<T>(dynamic, sensitive, key, label, description, defaultValue, configurationCategory,
					valueConverter, valueType, Arrays.asList(tags));
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

		public ConfigurationOptionBuilder<T> tags(String... tags) {
			this.tags = tags;
			return this;
		}

		/**
		 * Marks this ConfigurationOption as sensitive.
		 * <p/>
		 * If a value has sensitive content (e.g. password), it should be rendered
		 * as an input of type="password", rather then as type="text".
		 *
		 * @return <code>this</code>, for chaining.
		 */
		public ConfigurationOptionBuilder<T> sensitive() {
			this.sensitive = true;
			return this;
		}
	}
}
