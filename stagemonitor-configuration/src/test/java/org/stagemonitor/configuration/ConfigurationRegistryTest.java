package org.stagemonitor.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.stagemonitor.configuration.source.SimpleSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConfigurationRegistryTest {

	@Test
	public void testAddConfigurationSource() {
		ConfigurationRegistry configuration = new ConfigurationRegistry("");
		configuration.addConfigurationSource(new SimpleSource());

		assertEquals(Collections.singletonMap("Transient Configuration Source", true), configuration.getNamesOfConfigurationSources());
	}

	@Test
	public void testIsPasswordSetTrue() throws Exception {
		ConfigurationRegistry configuration = new ConfigurationRegistry("pwd");
		configuration.addConfigurationSource(SimpleSource.forTest("pwd", ""));
		assertTrue(configuration.isPasswordSet());
	}

	@Test
	public void testIsPasswordSetFalse() throws Exception {
		ConfigurationRegistry configuration = new ConfigurationRegistry("pwd");
		assertFalse(configuration.isPasswordSet());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDuplicateLabel() throws Exception {
		final ConfigurationOptionProvider optionProvider = TestConfigurationOptionProvider.of(
				ConfigurationOption.stringOption().key("foo").description("Foo").build(),
				ConfigurationOption.stringOption().key("foo").label("Bar").build());
		new ConfigurationRegistry(Collections.singletonList(optionProvider), Collections.emptyList(), "");
	}

	@Test
	public void testOnConfigurationChanged() throws Exception {
		AtomicBoolean changeListenerFired = new AtomicBoolean(false);
		final ConfigurationOption<String> configurationOption = ConfigurationOption.stringOption()
				.key("foo")
				.dynamic(true)
				.addChangeListener(new ConfigurationOption.ChangeListener<String>() {
					@Override
					public void onChange(ConfigurationOption<?> configurationOption, String oldValue, String newValue) {
						assertEquals("foo", configurationOption.getKey());
						assertEquals("old", oldValue);
						assertEquals("new", newValue);
						changeListenerFired.set(true);
						throw new RuntimeException("This is an expected test exception. " +
								"It is thrown to test whether Configuration can cope with change listeners that throw an exception.");
					}
				}).buildWithDefault("old");

		final ConfigurationOptionProvider optionProvider = TestConfigurationOptionProvider.of(configurationOption);
		final SimpleSource configurationSource = new SimpleSource("test");
		final ConfigurationRegistry config = new ConfigurationRegistry(Collections.singletonList(optionProvider), Collections.singletonList(configurationSource), "");
		config.save("foo", "new", "test");
		assertTrue(changeListenerFired.get());
	}

	@Test(expected = IllegalStateException.class)
	public void testFailOnRequiredValueMissing() throws Exception {
		final ConfigurationOption<String> configurationOption = ConfigurationOption.stringOption().key("foo").required().build();

		final ConfigurationOptionProvider optionProvider = TestConfigurationOptionProvider.of(configurationOption);
		new ConfigurationRegistry(Collections.singletonList(optionProvider), Collections.emptyList(), "", true);
	}

	@Test
	public void testValidateConfigurationOption() throws Exception {
		final ConfigurationOption<Boolean> configurationOption = ConfigurationOption.booleanOption()
				.key("foo")
				.addValidator(value -> {
					if (!value) {
						throw new IllegalArgumentException("Validation failed");
					}
				})
				.buildWithDefault(true);

		final ConfigurationOptionProvider optionProvider = TestConfigurationOptionProvider.of(configurationOption);
		final SimpleSource configurationSource = new SimpleSource("test");
		final ConfigurationRegistry config = new ConfigurationRegistry(Collections.singletonList(optionProvider), Collections.singletonList(configurationSource), "");
		try {
			config.save("foo", "false", "test");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Validation failed", e.getMessage());
		}
	}

	@Test
	public void testValidateDefaultConfigurationOption() throws Exception {
		try {
			ConfigurationOption.booleanOption()
					.key("foo")
					.addValidator(new ConfigurationOption.Validator<Boolean>() {
						@Override
						public void assertValid(Boolean value) {
							if (!value) {
								throw new IllegalArgumentException("Validation failed");
							}
						}
					})
					.buildWithDefault(false);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Validation failed", e.getMessage());
		}
	}

	@Test
	public void testToJsonDoesNotThrowException() throws Exception {
		final ConfigurationOptionProvider optionProvider = TestConfigurationOptionProvider.of(
				ConfigurationOption.stringOption().key("foo").description("Foo").configurationCategory("Foos").build());
		final ConfigurationRegistry configuration = new ConfigurationRegistry(Collections.singletonList(optionProvider), Collections.emptyList(), "");
		new ObjectMapper().writeValueAsString(configuration.getConfigurationOptionsByCategory());
	}

	private static class TestConfigurationOptionProvider extends ConfigurationOptionProvider {

		private final List<ConfigurationOption<?>> configurationOptions;

		public static ConfigurationOptionProvider of(ConfigurationOption<?>... configurationOptions) {
			return new TestConfigurationOptionProvider(configurationOptions);
		}

		private TestConfigurationOptionProvider(ConfigurationOption<?>... configurationOptions) {
			this.configurationOptions = Arrays.asList(configurationOptions);
		}

		@Override
		public List<ConfigurationOption<?>> getConfigurationOptions() {
			return configurationOptions;
		}
	}
}
