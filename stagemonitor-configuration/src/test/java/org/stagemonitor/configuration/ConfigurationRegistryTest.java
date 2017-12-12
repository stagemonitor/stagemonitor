package org.stagemonitor.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.stagemonitor.configuration.source.SimpleSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConfigurationRegistryTest {

	@Test
	public void testAddConfigurationSource() {
		ConfigurationRegistry configuration = ConfigurationRegistry.builder().build();
		configuration.addConfigurationSource(new SimpleSource());

		assertEquals(Collections.singletonMap("Transient Configuration Source", true), configuration.getNamesOfConfigurationSources());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDuplicateLabel() throws Exception {
		final ConfigurationOptionProvider optionProvider = TestConfigurationOptionProvider.of(
				ConfigurationOption.stringOption().key("foo").description("Foo").build(),
				ConfigurationOption.stringOption().key("foo").label("Bar").build());
		ConfigurationRegistry.builder().addOptionProvider(optionProvider).build();
	}

	@Test
	public void testOnConfigurationChanged() throws Exception {
		AtomicBoolean changeListenerFired = new AtomicBoolean(false);
		final ConfigurationOption<String> configurationOption = ConfigurationOption.stringOption()
				.key("foo")
				.dynamic(true)
				.addChangeListener((opt, oldValue, newValue) -> {
					assertEquals("foo", opt.getKey());
					assertEquals("old", oldValue);
					assertEquals("new", newValue);
					changeListenerFired.set(true);
					throw new RuntimeException("This is an expected test exception. " +
							"It is thrown to test whether Configuration can cope with change listeners that throw an exception.");
				}).buildWithDefault("old");

		final ConfigurationOptionProvider optionProvider = TestConfigurationOptionProvider.of(configurationOption);
		final SimpleSource configurationSource = new SimpleSource("test");
		final ConfigurationRegistry config = ConfigurationRegistry.builder()
				.addOptionProvider(optionProvider)
				.addConfigSource(configurationSource)
				.build();
		config.save("foo", "new", "test");
		assertTrue(changeListenerFired.get());
	}

	@Test(expected = IllegalStateException.class)
	public void testFailOnRequiredValueMissing() throws Exception {
		final ConfigurationOption<String> configurationOption = ConfigurationOption.stringOption().key("foo").buildRequired();

		final ConfigurationOptionProvider optionProvider = TestConfigurationOptionProvider.of(configurationOption);
		ConfigurationRegistry.builder().addOptionProvider(optionProvider).failOnMissingRequiredValues(true).build();
	}

	@Test
	public void testValidateConfigurationOption() throws Exception {
		final ConfigurationOption<Boolean> configurationOption = ConfigurationOption.booleanOption()
				.key("foo")
				.addValidator((value) -> {
					if (!value) {
						throw new IllegalArgumentException("Validation failed");
					}
				})
				.buildWithDefault(true);

		final ConfigurationOptionProvider optionProvider = TestConfigurationOptionProvider.of(configurationOption);
		final SimpleSource configurationSource = new SimpleSource("test");
		final ConfigurationRegistry config = ConfigurationRegistry.builder()
				.addOptionProvider(optionProvider)
				.addConfigSource(configurationSource)
				.build();
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
					.addValidator((value) -> {
						if (!value) {
							throw new IllegalArgumentException("Validation failed");
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
		final ConfigurationRegistry configuration = ConfigurationRegistry.builder().addOptionProvider(optionProvider).build();
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
