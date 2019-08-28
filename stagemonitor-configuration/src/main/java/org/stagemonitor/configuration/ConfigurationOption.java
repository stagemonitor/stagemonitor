package org.stagemonitor.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.converter.BooleanValueConverter;
import org.stagemonitor.configuration.converter.ClassInstanceValueConverter;
import org.stagemonitor.configuration.converter.DoubleValueConverter;
import org.stagemonitor.configuration.converter.EnumValueConverter;
import org.stagemonitor.configuration.converter.IntegerValueConverter;
import org.stagemonitor.configuration.converter.JsonValueConverter;
import org.stagemonitor.configuration.converter.ListValueConverter;
import org.stagemonitor.configuration.converter.LongValueConverter;
import org.stagemonitor.configuration.converter.MapValueConverter;
import org.stagemonitor.configuration.converter.OptionalValueConverter;
import org.stagemonitor.configuration.converter.RegexValueConverter;
import org.stagemonitor.configuration.converter.SetValueConverter;
import org.stagemonitor.configuration.converter.StringValueConverter;
import org.stagemonitor.configuration.converter.UrlValueConverter;
import org.stagemonitor.configuration.converter.ValueConverter;
import org.stagemonitor.configuration.source.ConfigurationSource;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
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
	// key: validOptionAsString, value: label
	private final Map<String, String> validOptions;
	private volatile List<ConfigurationSource> configurationSources;
	private volatile ConfigurationRegistry configuration;
	private volatile String errorMessage;
	private volatile OptionValue<T> optionValue;

	public static class OptionValue<T> {
		private final T value;
		private final String valueAsString;
		private final String nameOfCurrentConfigurationSource;
		private final String usedKey;

		OptionValue(T value, String valueAsString, String nameOfCurrentConfigurationSource, String usedKey) {
			this.value = value;
			this.valueAsString = valueAsString;
			this.nameOfCurrentConfigurationSource = nameOfCurrentConfigurationSource;
			this.usedKey = usedKey;
		}

		public T getValue() {
			return value;
		}

		public String getValueAsString() {
			return valueAsString;
		}

		public String getNameOfCurrentConfigurationSource() {
			return nameOfCurrentConfigurationSource;
		}

		public String getUsedKey() {
			return usedKey;
		}
	}

	public static <T> ConfigurationOptionBuilder<T> builder(ValueConverter<T> valueConverter, Class<? super T> valueType) {
		return new ConfigurationOptionBuilder<T>(valueConverter, valueType);
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link String}
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link String}
	 */
	public static ConfigurationOptionBuilder<String> stringOption() {
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
	public static ConfigurationOptionBuilder<Boolean> booleanOption() {
		return new ConfigurationOptionBuilder<Boolean>(BooleanValueConverter.INSTANCE, Boolean.class);
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link Integer}
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link Integer}
	 */
	public static ConfigurationOptionBuilder<Integer> integerOption() {
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
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link List}&lt;{@link String}&gt;
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link List}&lt;{@link String}&gt;
	 */
	public static ConfigurationOptionBuilder<Collection<String>> stringsOption() {
		return new ConfigurationOptionBuilder<Collection<String>>(SetValueConverter.STRINGS_VALUE_CONVERTER, Collection.class)
				.defaultValue(Collections.<String>emptySet());
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link List}&lt;{@link String}&gt; and all
	 * Strings are converted to lower case.
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link List}&lt;{@link String}&gt;
	 */
	public static ConfigurationOptionBuilder<Collection<String>> lowerStringsOption() {
		return new ConfigurationOptionBuilder<Collection<String>>(SetValueConverter.LOWER_STRINGS_VALUE_CONVERTER, Collection.class)
				.defaultValue(Collections.<String>emptySet());
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link Set}&lt;{@link Integer}&gt;
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link Set}&lt;{@link Integer}&gt;
	 */
	public static ConfigurationOption.ConfigurationOptionBuilder<Collection<Integer>> integersOption() {
		return ConfigurationOption.builder(SetValueConverter.INTEGERS, Collection.class);
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link List}&lt;{@link Pattern}&gt;
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link List}&lt;{@link Pattern}&gt;
	 */
	public static ConfigurationOptionBuilder<Collection<Pattern>> regexListOption() {
		return new ConfigurationOptionBuilder<Collection<Pattern>>(new SetValueConverter<Pattern>(RegexValueConverter.INSTANCE), Collection.class)
				.defaultValue(Collections.<Pattern>emptySet());
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link Map}&lt;{@link Pattern}, {@link
	 * String}&gt;
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link Map}&lt;{@link Pattern}, {@link
	 * String}&gt;
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
		final ConfigurationOptionBuilder<T> optionBuilder = new ConfigurationOptionBuilder<T>(new EnumValueConverter<T>(clazz), clazz);
		for (T enumConstant : clazz.getEnumConstants()) {
			optionBuilder.addValidOption(enumConstant);
		}
		optionBuilder.sealValidOptions();
		return optionBuilder;
	}

	/**
	 * Adds a configuration option for intended classes loaded by {@link ServiceLoader}s
	 *
	 * <p>Restricts the {@link #validOptions} to the class names of the {@link ServiceLoader} implementations of the
	 * provided service loader interface.</p>
	 *
	 * <p> Note that the implementations have to be registered in {@code META-INF/services/{serviceLoaderInterface.getName()}}</p>
	 */
	public static <T> ConfigurationOptionBuilder<T> serviceLoaderStrategyOption(Class<T> serviceLoaderInterface) {
		final ConfigurationOptionBuilder<T> optionBuilder = new ConfigurationOptionBuilder<T>(ClassInstanceValueConverter.of(serviceLoaderInterface), serviceLoaderInterface);
		for (T impl : ServiceLoader.load(serviceLoaderInterface, ConfigurationOption.class.getClassLoader())) {
			optionBuilder.addValidOption(impl);
		}
		optionBuilder.sealValidOptions();
		return optionBuilder;
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link String}
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link String}
	 */
	public static ConfigurationOptionBuilder<URL> urlOption() {
		return new ConfigurationOptionBuilder<URL>(UrlValueConverter.INSTANCE, URL.class);
	}

	/**
	 * Constructs a {@link ConfigurationOptionBuilder} whose value is of type {@link String}
	 *
	 * @return a {@link ConfigurationOptionBuilder} whose value is of type {@link String}
	 */
	public static ConfigurationOptionBuilder<List<URL>> urlsOption() {
		return new ConfigurationOptionBuilder<List<URL>>(new ListValueConverter<URL>(UrlValueConverter.INSTANCE), List.class)
				.defaultValue(Collections.<URL>emptyList());
	}

	private ConfigurationOption(boolean dynamic, boolean sensitive, String key, String label, String description,
								T defaultValue, String configurationCategory, final ValueConverter<T> valueConverter,
								Class<? super T> valueType, List<String> tags, boolean required,
								List<ChangeListener<T>> changeListeners, List<Validator<T>> validators,
								List<String> aliasKeys, final Map<String, String> validOptions) {
		this.dynamic = dynamic;
		this.key = key;
		this.aliasKeys = aliasKeys;
		this.label = label;
		this.description = description;
		this.defaultValue = defaultValue;
		this.tags = tags;
		validators = new ArrayList<Validator<T>>(validators);
		if (validOptions != null) {
			this.validOptions = Collections.unmodifiableMap(new LinkedHashMap<String, String>(validOptions));
			validators.add(new ValidOptionValidator<T>(validOptions.keySet(), valueConverter));
		} else {
			this.validOptions = null;
		}
		this.validators = Collections.unmodifiableList(new ArrayList<Validator<T>>(validators));
		this.defaultValueAsString = valueConverter.toString(defaultValue);
		this.configurationCategory = configurationCategory;
		this.valueConverter = valueConverter;
		this.valueType = valueType;
		this.sensitive = sensitive;
		this.required = required;
		this.changeListeners = new CopyOnWriteArrayList<ChangeListener<T>>(changeListeners);
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
		return optionValue.valueAsString;
	}

	public String getValueAsSafeString() {
		return valueConverter.toSafeString(optionValue.value);
	}

	/**
	 * Returns <code>true</code>, if the value is sensitive, <code>false</code> otherwise. If a value has sensitive
	 * content (e.g. password), it should be rendered as an input of type="password", rather then as type="text".
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
		return optionValue.value;
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

	/**
	 * Returns an immutable snapshot of the {@link OptionValue}.
	 * This makes sure to get a consistent snapshot of all the related values without the risk of partially applied updates.
	 * This means that {@link OptionValue#getValue()} and {@link OptionValue#getValueAsString()} are always in sync,
	 * which is not guaranteed if subsequently calling {@link #getValue()} and {@link #getValueAsString()}
	 * because there could be a concurrent update between those reads.
	 *
	 * @return an immutable snapshot of the {@link OptionValue}
	 */
	@JsonIgnore
	public OptionValue<T> getOptionValue() {
		return optionValue;
	}

	void setConfigurationSources(List<ConfigurationSource> configurationSources) {
		this.configurationSources = configurationSources;
		loadValue();
	}

	void setConfiguration(ConfigurationRegistry configuration) {
		this.configuration = configuration;
	}

	/**
	 * Returns the name of the configuration source that provided the current value
	 *
	 * @return the name of the configuration source that provided the current value
	 */
	public String getNameOfCurrentConfigurationSource() {
		return optionValue.nameOfCurrentConfigurationSource;
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

	/**
	 * Returns the valid values for this configuration option
	 *
	 * @return the valid values for this configuration option
	 */
	public Collection<String> getValidOptions() {
		if (validOptions == null) {
			return null;
		}
		return validOptions.keySet();
	}

	public Map<String, String> getValidOptionsLabelMap() {
		return validOptions;
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
			success = trySetValue(configValueInfo, key);
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

	private boolean trySetValue(ConfigValueInfo configValueInfo, String key) {
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
				setValue(newValue, newValueAsString, newConfigurationSourceName, key);
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
		setValue(defaultValue, defaultValueAsString, "Default Value", key);
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
		return !property.equals(optionValue.valueAsString);
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

	private void setValue(T value, String valueAsString, String nameOfCurrentConfigurationSource, String key) {
		for (Validator<T> validator : validators) {
			validator.assertValid(value);
		}
		this.optionValue = new OptionValue<T>(value, valueAsString, nameOfCurrentConfigurationSource, key);
	}

	/**
	 * Returns true if the current value is equal to the default value
	 *
	 * @return true if the current value is equal to the default value
	 */
	public boolean isDefault() {
		return (optionValue.valueAsString != null && optionValue.valueAsString.equals(defaultValueAsString)) ||
				(optionValue.valueAsString == null && defaultValueAsString == null);
	}

	public void addChangeListener(ChangeListener<T> changeListener) {
		changeListeners.add(changeListener);
	}

	public boolean removeChangeListener(ChangeListener<T> changeListener) {
		return changeListeners.remove(changeListener);
	}

	/**
	 * A {@link ConfigurationOption} can have multiple keys ({@link #key} and {@link #aliasKeys}). This method returns
	 * the key which is used by the current configuration source ({@link #nameOfCurrentConfigurationSource}).
	 *
	 * @return the used key of the current configuration source
	 */
	public String getUsedKey() {
		return optionValue.usedKey;
	}

	/**
	 * Notifies about configuration changes
	 */
	public interface ChangeListener<T> {
		/**
		 * @param configurationOption the configuration option which has just changed its value
		 * @param oldValue            the old value
		 * @param newValue            the new value
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
		 *
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
		private Map<String, String> validOptions;
		private boolean validOptionsSealed = false;

		private ConfigurationOptionBuilder(ValueConverter<T> valueConverter, Class<? super T> valueType) {
			this.valueConverter = valueConverter;
			this.valueType = valueType;
		}

		/**
		 * Be aware that when using this method you might have to deal with <code>null</code> values when calling {@link
		 * #getValue()}. That's why this method is deprecated
		 *
		 * @deprecated use {@link #buildRequired()}, {@link #buildWithDefault(Object)} or {@link #buildOptional()}. The
		 * only valid use of this method is if {@link #buildOptional()} would be the semantically correct option but you
		 * are not using Java 8+.
		 */
		@Deprecated
		public ConfigurationOption<T> build() {
			return new ConfigurationOption<T>(dynamic, sensitive, key, label, description, defaultValue, configurationCategory,
					valueConverter, valueType, Arrays.asList(tags), required,
					changeListeners,
					validators,
					Arrays.asList(aliasKeys), validOptions);
		}

		/**
		 * Builds the option and marks it as required.
		 *
		 * <p> Use this method if you don't want to provide a default value but setting a value is still required. You
		 * will have to make sure to provide a value is present on startup. </p>
		 *
		 * <p> When a required option does not have a value the behavior depends on {@link
		 * ConfigurationRegistry#failOnMissingRequiredValues}. Either an {@link IllegalStateException} is raised, which
		 * can potentially prevent the application form starting or a warning gets logged. </p>
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
		 *
		 * <p> Use this method if setting this option is not required and to express that it may be <code>null</code>.
		 * </p>
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
					java.util.Optional.ofNullable(defaultValue), configurationCategory,
					new OptionalValueConverter<T>(valueConverter), java.util.Optional.class, Arrays.asList(this.tags), required,
					optionalChangeListeners, optionalValidators, Arrays.asList(aliasKeys), validOptions);
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
		 *
		 * <p> If a value has sensitive content (e.g. password), it should be rendered as an input of type="password",
		 * rather then as type="text". </p>
		 *
		 * @return <code>this</code>, for chaining.
		 */
		public ConfigurationOptionBuilder<T> sensitive() {
			this.sensitive = true;
			return this;
		}

		/**
		 * Marks this option as required.
		 *
		 * <p> When a required option does not have a value the behavior depends on {@link
		 * ConfigurationRegistry#failOnMissingRequiredValues}. Either an {@link IllegalStateException} is raised, which
		 * can potentially prevent the application form starting or a warning gets logged. </p>
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

		public ConfigurationOptionBuilder<T> validOptions(List<T> options) {
			for (T option : options) {
				addValidOption(option);
			}
			return this;
		}

		public ConfigurationOptionBuilder<T> addValidOptions(T... options) {
			for (T option : options) {
				addValidOption(option);
			}
			return this;
		}

		public ConfigurationOptionBuilder<T> addValidOption(T option) {
			if (option instanceof Collection) {
				throw new UnsupportedOperationException("Adding valid options to a collection option is not supported. " +
						"If you need this feature please raise an issue describing your use case.");
			} else {
				final String validOptionAsString = valueConverter.toString(option);
				addValidOptionAsString(validOptionAsString, getLabel(option, validOptionAsString));
			}
			return this;
		}

		private String getLabel(Object option, String defaultLabel) {
			if (overridesToString(option)) {
				return option.toString();
			}
			return defaultLabel;
		}

		private boolean overridesToString(Object o) {
			try {
				return o.getClass().getDeclaredMethod("toString").getDeclaringClass() != Object.class;
			} catch (NoSuchMethodException e) {
				return false;
			}
		}

		@SuppressWarnings("unchecked")
		private T getSingleValue(Object o) {
			return (T) Collections.singletonList(o);
		}

		private ConfigurationOptionBuilder<T> addValidOptionAsString(String validOptionAsString, String label) {
			if (validOptionsSealed) {
				throw new IllegalStateException("Options are sealed, you can't add any new ones");
			}
			if (validOptions == null) {
				validOptions = new LinkedHashMap<String, String>();
			}
			validOptions.put(validOptionAsString, label);
			return this;
		}

		/**
		 * Makes sure that no more valid options can be added
		 *
		 * @return this, for chaining
		 */
		public ConfigurationOptionBuilder<T> sealValidOptions() {
			this.validOptionsSealed = true;
			if (validOptions != null) {
				validOptions = Collections.unmodifiableMap(validOptions);
			}
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

	private static class ValidOptionValidator<T> implements Validator<T> {
		private final Set<String> validOptions;
		private final ValueConverter<T> valueConverter;

		ValidOptionValidator(Collection<String> validOptions, ValueConverter<T> valueConverter) {
			this.validOptions = new HashSet<String>(validOptions);
			this.valueConverter = valueConverter;
		}

		@Override
		public void assertValid(T value) {
			String valueAsString = valueConverter.toString(value);
			if (!validOptions.contains(valueAsString)) {
				throw new IllegalArgumentException("Invalid option '" + valueAsString + "' expecting one of " + validOptions);
			}
		}
	}
}
