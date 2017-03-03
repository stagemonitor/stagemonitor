package org.stagemonitor.core.configuration;

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
import org.stagemonitor.core.configuration.converter.OptionalValueConverter;
import org.stagemonitor.core.configuration.converter.RegexValueConverter;
import org.stagemonitor.core.configuration.converter.SetValueConverter;
import org.stagemonitor.core.configuration.converter.StringValueConverter;
import org.stagemonitor.core.configuration.converter.ValueConverter;
import org.stagemonitor.core.configuration.source.ConfigurationSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

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
	private final List<String> aliasKeys;
	private final List<String> allKeys;
	private final String label;
	private final String description;
	private final T defaultValue;
	private final List<String> tags;
	private final List<Validator<T>> validators;
	private final List<ChangeListener<T>> changeListeners;
	private final boolean required;
	private final String defaultValueAsString;
	private final String configurationCategory;
	@JsonIgnore
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
								Class<? super T> valueType, List<String> tags, boolean required,
								List<ChangeListener<T>> changeListeners, List<Validator<T>> validators,
								List<String> aliasKeys) {
		this.dynamic = dynamic;
		this.key = key;
		this.aliasKeys = aliasKeys;
		this.label = label;
		this.description = description;
		this.defaultValue = defaultValue;
		this.tags = tags;
		this.defaultValueAsString = valueConverter.toString(defaultValue);
		this.configurationCategory = configurationCategory;
		this.valueConverter = valueConverter;
		this.valueType = valueType;
		this.sensitive = sensitive;
		this.required = required;
		this.changeListeners = changeListeners;
		this.validators = validators;
		setToDefault();
		final ArrayList<String> tempAllKeys = new ArrayList<String>(aliasKeys.size() + 1);
		tempAllKeys.add(key);
		tempAllKeys.addAll(aliasKeys);
		this.allKeys = Collections.unmodifiableList(tempAllKeys);
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
	 * Returns the alternate keys of the configuration option that can for example be used in a properties file
	 *
	 * @return the alternate config keys
	 */
	public List<String> getAliasKeys() {
		return Collections.unmodifiableList(aliasKeys);
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
		return getValue();
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

	public ValueConverter<T> getValueConverter() {
		return valueConverter;
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
		boolean success = false;
		for (String key : allKeys) {
			ConfigValueInfo configValueInfo = loadValueFromSources(key);
			success = trySetValue(configValueInfo);
			if (success) {
				break;
			}
		}
		if (!success) {
			setToDefault();
		}
	}

	private ConfigValueInfo loadValueFromSources(String key) {
		for (ConfigurationSource configurationSource : configurationSources) {
			String newValueAsString = configurationSource.getValue(key);
			if (newValueAsString != null) {
				return new ConfigValueInfo(newValueAsString, configurationSource.getName());
			}
		}
		return new ConfigValueInfo();
	}

	private boolean trySetValue(ConfigValueInfo configValueInfo) {
		final String newConfigurationSourceName = configValueInfo.getNewConfigurationSourceName();
		String newValueAsString = configValueInfo.getNewValueAsString();
		if (newValueAsString == null) {
			return false;
		}
		newValueAsString = newValueAsString.trim();
		T oldValue = getValue();
		if (hasChanges(newValueAsString)) {
			try {
				final T newValue = valueConverter.convert(newValueAsString);
				setValue(newValue, newValueAsString, newConfigurationSourceName);
				errorMessage = null;
				if (isInitialized()) {
					for (ChangeListener<T> changeListener : changeListeners) {
						try {
							changeListener.onChange(this, oldValue, getValue());
						} catch (RuntimeException e) {
							logger.warn(e.getMessage() + " (this exception is ignored)", e);
						}
					}
				}
				return true;
			} catch (IllegalArgumentException e) {
				errorMessage = "Error in " + newConfigurationSourceName + ": " + e.getMessage();
				logger.warn(errorMessage + " Default value '" + defaultValueAsString + "' for '" + key + "' will be applied.");
				return false;
			}
		} else {
			return true;
		}
	}

	private void setToDefault() {
		final String msg = "Missing required value for configuration option " + key;
		if (isInitialized() && required && defaultValue == null) {
			handleMissingRequiredValue(msg);
		}
		setValue(defaultValue, defaultValueAsString, "Default Value");
	}

	private boolean isInitialized() {
		return configuration != null;
	}

	private void handleMissingRequiredValue(String msg) {
		if (configuration.isFailOnMissingRequiredValues()) {
			throw new IllegalStateException(msg);
		} else {
			logger.warn(msg);
		}
	}

	private boolean hasChanges(String property) {
		return !property.equals(valueAsString);
	}

	/**
	 * Throws a {@link IllegalArgumentException} if the value is not valid
	 *
	 * @param valueAsString the configuration value as string
	 * @throws IllegalArgumentException if there was a error while converting the value
	 */
	public void assertValid(String valueAsString) throws IllegalArgumentException {
		final T value = valueConverter.convert(valueAsString);
		for (Validator<T> validator : validators) {
			validator.assertValid(value);
		}
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

	private void setValue(T value, String valueAsString, String nameOfCurrentConfigurationSource) {
		for (Validator<T> validator : validators) {
			validator.assertValid(value);
		}

		this.value = value;
		this.valueAsString = valueAsString;
		this.nameOfCurrentConfigurationSource = nameOfCurrentConfigurationSource;
	}

	/**
	 * @throws NoClassDefFoundError When not using Java 8+
	 */
	public Supplier<T> asSupplier() {
		return new Supplier<T>() {
			@Override
			public T get() {
				return getValue();
			}
		};
	}

	/**
	 * Notifies about configuration changes
	 */
	public interface ChangeListener<T> {
		/**
		 *
		 * @param configurationOption the configuration option which has just changed its value
		 * @param oldValue the old value
		 * @param newValue the new value
		 */
		void onChange(ConfigurationOption<?> configurationOption, T oldValue, T newValue);

		class OptionalChangeListenerAdapter<T> implements ChangeListener<Optional<T>> {

			private final ChangeListener<T> changeListener;

			public OptionalChangeListenerAdapter(ChangeListener<T> changeListener) {
				this.changeListener = changeListener;
			}

			@Override
			public void onChange(ConfigurationOption<?> configurationOption, Optional<T> oldValue, Optional<T> newValue) {
				changeListener.onChange(configurationOption, oldValue.orElse(null), newValue.orElse(null));
			}
		}
	}

	public interface Validator<T> {
		/**
		 * Validates a value
		 * @param value the value to be validated
		 * @throws IllegalArgumentException if the value is invalid
		 */
		void assertValid(T value);

		class OptionalValidatorAdapter<T> implements Validator<Optional<T>> {
			private final Validator<T> validator;

			public OptionalValidatorAdapter(Validator<T> validator) {
				this.validator = validator;
			}

			@Override
			public void assertValid(Optional<T> value) {
				validator.assertValid(value.orElse(null));
			}
		}
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
		private boolean required = false;
		private List<ChangeListener<T>> changeListeners = new ArrayList<ChangeListener<T>>();
		private List<Validator<T>> validators = new ArrayList<Validator<T>>();
		private String[] aliasKeys = new String[0];

		private ConfigurationOptionBuilder(ValueConverter<T> valueConverter, Class<? super T> valueType) {
			this.valueConverter = valueConverter;
			this.valueType = valueType;
		}

		/**
		 * Be aware that when using this method you might have to deal with <code>null</code> values when calling {@link
		 * #getValue()}.
		 * <p/> That's why this method is deprecated
		 *
		 * @deprecated use {@link #buildRequired()}, {@link #buildWithDefault(Object)} or {@link #buildOptional()}. The
		 * only valid use of this method is if {@link #buildOptional()} would be the semantically correct option but you
		 * are not using Java 8+.
		 */
		@Deprecated
		public ConfigurationOption<T> build() {
			return new ConfigurationOption<T>(dynamic, sensitive, key, label, description, defaultValue, configurationCategory,
					valueConverter, valueType, Arrays.asList(tags), required,
					Collections.unmodifiableList(changeListeners),
					Collections.unmodifiableList(validators),
					Arrays.asList(aliasKeys));
		}

		/**
		 * Builds the option and marks it as required.
		 * <p/>
		 * Use this method if you don't want to provide a default value but setting a value is still required. You
		 * will have to make sure to provide a value is present on startup.
		 * <p/>
		 * When a required option does not have a value the behavior depends on
		 * {@link Configuration#failOnMissingRequiredValues}. Either an {@link IllegalStateException} is raised,
		 * which can potentially prevent the application form starting or a warning gets logged.
		 */
		public ConfigurationOption<T> buildRequired() {
			this.required = true;
			return build();
		}

		/**
		 * Builds the option with a default value so that {@link ConfigurationOption#getValue()} will never return
		 * <code>null</code>
		 *
		 * @param defaultValue The default value which has to be non-<code>null</code>
		 * @throws IllegalArgumentException When <code>null</code> was provided
		 */
		public ConfigurationOption<T> buildWithDefault(T defaultValue) {
			if (defaultValue == null) {
				throw new IllegalArgumentException("Default value must not be null");
			}
			this.required = true;
			this.defaultValue = defaultValue;
			return build();
		}

		/**
		 * Builds the option and marks it as not required
		 * <p/>
		 * Use this method if setting this option is not required and to express that it may be <code>null</code>.
		 *
		 * @throws NoClassDefFoundError When not using Java 8+
		 */
		public ConfigurationOption<Optional<T>> buildOptional() {
			required = false;
			final List<ChangeListener<Optional<T>>> optionalChangeListeners = new ArrayList<ChangeListener<Optional<T>>>(changeListeners.size());
			for (ChangeListener<T> changeListener : changeListeners) {
				optionalChangeListeners.add(new ChangeListener.OptionalChangeListenerAdapter<T>(changeListener));
			}
			final List<Validator<Optional<T>>> optionalValidators = new ArrayList<Validator<Optional<T>>>(validators.size());
			for (Validator<T> validator : validators) {
				optionalValidators.add(new Validator.OptionalValidatorAdapter<T>(validator));
			}
			return new ConfigurationOption<Optional<T>>(dynamic, sensitive, key, label, description,
					Optional.ofNullable(defaultValue), configurationCategory,
					new OptionalValueConverter<T>(valueConverter), Optional.class, Arrays.asList(this.tags), required,
					optionalChangeListeners, optionalValidators, Arrays.asList(aliasKeys));
		}

		public ConfigurationOptionBuilder<T> dynamic(boolean dynamic) {
			this.dynamic = dynamic;
			return this;
		}

		public ConfigurationOptionBuilder<T> key(String key) {
			this.key = key;
			return this;
		}

		/**
		 * Sets alternate keys of the configuration option which act as an alias for the primary {@link #key(String)}
		 *
		 * @return <code>this</code>, for chaining.
		 */
		public ConfigurationOptionBuilder<T> aliasKeys(String... aliasKeys) {
			this.aliasKeys = aliasKeys;
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

		/**
		 * @deprecated use {@link #buildWithDefault(Object)}
		 */
		@Deprecated
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

		/**
		 * Marks this option as required.
		 * <p/>
		 * When a required option does not have a value the behavior depends on
		 * {@link Configuration#failOnMissingRequiredValues}. Either an {@link IllegalStateException} is raised,
		 * which can potentially prevent the application form starting or a warning gets logged.
		 *
		 * @return <code>this</code>, for chaining.
		 * @deprecated use {@link #buildRequired()}
		 */
		@Deprecated
		public ConfigurationOptionBuilder<T> required() {
			this.required = true;
			return this;
		}

		public ConfigurationOptionBuilder<T> addChangeListener(ChangeListener<T> changeListener) {
			this.changeListeners.add(changeListener);
			return this;
		}

		public ConfigurationOptionBuilder<T> addValidator(Validator<T> validator) {
			this.validators.add(validator);
			return this;
		}

	}

	private static class ConfigValueInfo {
		private String newValueAsString;
		private String newConfigurationSourceName;

		private ConfigValueInfo() {
		}

		private ConfigValueInfo(String newValueAsString, String newConfigurationSourceName) {
			this.newValueAsString = newValueAsString;
			this.newConfigurationSourceName = newConfigurationSourceName;
		}

		private String getNewValueAsString() {
			return newValueAsString;
		}

		private String getNewConfigurationSourceName() {
			return newConfigurationSourceName;
		}
	}
}
