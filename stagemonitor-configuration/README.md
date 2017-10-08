# Type safe configuration management

This library does not depend on stagemonitor and can be integrated into any project which wants to take advantage of a advanced configuration management system.

## Features
 - Type safety - accessing configuration options in a completely type safe way.
   - Includes `ValueConverters` for the most common data types
   - You can implement custom `ValueConverters`
 - Null safety - `NullPointerExceptions` impossible when accessing a configuration option as it can't return `null`s by design. Includes support for `java.util.Optional`.
 - Validation - verify the integrity of your configuration. Want a int with a range from 1-42? No problem!
 - Change listeners - get notified when a configuration option changes
 - Loading values from different configuration sources. You can write custom configuration sources or use built in:
   - SystemPropertyConfigurationSource 
   - EnvironmentVariableConfigurationSource 
   - PropertyFileConfigurationSource 
   - ElasticsearchConfigurationSource 
   - SimpleSource (a simple ConcurrentHashMap<String, String>)
 - Easily testable by mocking configuration classes
 - Supports auto generating a configuration management UI
   - See all available configuration options
   - See documentation for the options
   - Change options at runtime and save it into a specific configuration source
   - See which value is loaded from which configuration source
   

![](http://www.stagemonitor.org/images/widget-configuration-1d36641e.png)


## Get started

Just add a dependency to the latest version: [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.stagemonitor/stagemonitor-web-servlet/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.stagemonitor/stagemonitor-configuration) 


```java
package org.stagemonitor.configuration.example;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationOptionProvider;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.source.EnvironmentVariableConfigurationSource;
import org.stagemonitor.configuration.source.PropertyFileConfigurationSource;
import org.stagemonitor.configuration.source.SimpleSource;
import org.stagemonitor.configuration.source.SystemPropertyConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Example {

	private ExampleConfiguration exampleConfiguration;
	private ConfigurationRegistry configurationRegistry;

	@Before
	public void setUp() throws Exception {
		configurationRegistry = new ConfigurationRegistry(
				// its also possible to automatically detect all ConfigurationOptionProvider
				// implementations at runtime via ServiceLoader.load
				Collections.singletonList(new ExampleConfiguration()),
				Arrays.asList(
						// Defines the hierarchy of configuration sources
						// The first one has the highest precedence
						// You can implement custom configuration sources
						// For example JdbcConfigurationSource to sore config values in your DB
						new SimpleSource(),
						new SystemPropertyConfigurationSource(),
						new PropertyFileConfigurationSource("application.properties"),
						new EnvironmentVariableConfigurationSource()
				),
				null);

		exampleConfiguration = configurationRegistry.getConfig(ExampleConfiguration.class);
	}

	/*
	 * You can group you configuration values into configuration classes
	 *
	 * This is especially useful if you are embracing a modular architecture approach - each module can have it's own
	 * configuration class
	 */
	public static class ExampleConfiguration extends ConfigurationOptionProvider {
		private static final String EXAMPLE_CATEGORY = "Example category";
		private final ConfigurationOption<Boolean> booleanExample = ConfigurationOption.booleanOption()
				.key("example.boolean")
				// explicitly flag this configuration option as dynamic which means we can change the value at runtime
				// Non dynamic options can't be saved to a transient configuration source
				// see org.stagemonitor.configuration.source.ConfigurationSource.isSavingPersistent()
				.dynamic(true)
				// "forces" you to document the purpose of this configuration option
				// You can even use this data to automatically generate a configuration UI
				.label("Example boolean config")
				.description("More detailed description of the configuration option")
				// categorize your config options. This is especially useful when generating a configuration UI
				.configurationCategory(EXAMPLE_CATEGORY)
				.tags("fancy", "wow")
				// configuration options can never return null values
				// as we have to either set a default value,
				// explicitly mark it as optional with buildOptional(), transforming the configuration option into a java.util.Optional
				// or require that a value has to be present in any configuration source (buildRequired())
				.buildWithDefault(true);

		private final ConfigurationOption<Optional<String>> optionalExample = ConfigurationOption.stringOption()
				.key("example.optional")
				.dynamic(true)
				.label("Example optional config")
				.configurationCategory(EXAMPLE_CATEGORY)
				.buildOptional();

		private final ConfigurationOption<Optional<String>> valitatorExample = ConfigurationOption.stringOption()
				.key("example.validator")
				.dynamic(false)
				.label("Example config with validator")
				.addValidator(value -> {
					if (value != null && !value.equals(value.toLowerCase())) {
						throw new IllegalArgumentException("Must be in lower case");
					}
				})
				.configurationCategory(EXAMPLE_CATEGORY)
				.buildOptional();

		public boolean getBooleanExample() {
			return booleanExample.get();
		}

		public ConfigurationOption<Optional<String>> getOptionalExample() {
			return optionalExample;
		}
	}

	@Test
	public void getConfigurationValueTypeSafe() throws Exception {
		assertThat(exampleConfiguration.getBooleanExample()).isTrue();
	}

	@Test
	public void testChangeListener() throws Exception {
		AtomicBoolean changeListenerInvoked = new AtomicBoolean(false);
		exampleConfiguration.getOptionalExample()
				.addChangeListener((configurationOption, oldValue, newValue) -> changeListenerInvoked.set(true));
		// saves a value into a specific configuration source
		configurationRegistry.save("example.optional", "foo", SimpleSource.NAME);
		assertThat(changeListenerInvoked).isTrue();
	}

	@Test
	public void testValidation() throws Exception {
		assertThatThrownBy(() -> configurationRegistry.save("example.validator", "FOO", SimpleSource.NAME))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Must be in lower case");
	}

	@Test
	public void testMocking() throws Exception {
		class SomeClassRequiringConfiguration {
			private final ExampleConfiguration exampleConfiguration;

			private SomeClassRequiringConfiguration(ExampleConfiguration exampleConfiguration) {
				this.exampleConfiguration = exampleConfiguration;
			}

			public String getFooOrBar() {
				if (exampleConfiguration.getBooleanExample()) {
					return "foo";
				} else {
					return "bar";
				}
			}
		}
		// For unit tests, it can be handy to mock the configuration classes
		ExampleConfiguration exampleConfigurationMock = mock(ExampleConfiguration.class);
		when(exampleConfigurationMock.getBooleanExample()).thenReturn(true);

		final SomeClassRequiringConfiguration someClass = new SomeClassRequiringConfiguration(exampleConfigurationMock);
		assertThat(someClass.getFooOrBar()).isEqualTo("foo");
	}
}

```


## Integration into Spring

TODO Inject configuration classes 


