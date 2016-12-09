package org.stagemonitor.core.configuration;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.source.ConfigurationSource;
import org.stagemonitor.core.configuration.source.SimpleSource;
import org.stagemonitor.core.configuration.source.SystemPropertyConfigurationSource;
import org.stagemonitor.core.util.JsonUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.Stagemonitor.STAGEMONITOR_PASSWORD;

public class ConfigurationTest {

	private Configuration configuration;
	private CorePlugin corePlugin;

	@Before
	public void init() {
		configuration = new Configuration(Collections.singletonList(new CorePlugin()),
				Collections.<ConfigurationSource>singletonList(new SimpleSource()), STAGEMONITOR_PASSWORD);
		corePlugin = configuration.getConfig(CorePlugin.class);
	}

	@Test
	public void testGetConfigSubclass() {
		final CorePlugin corePluginMock = mock(CorePlugin.class);
		configuration = new Configuration(Collections.singletonList(corePluginMock),
				Collections.<ConfigurationSource>singletonList(new SimpleSource()), STAGEMONITOR_PASSWORD);
		assertSame(corePluginMock, configuration.getConfig(CorePlugin.class));
		assertNull(configuration.getConfig(new ConfigurationOptionProvider(){}.getClass()));
	}

	@Test
	public void testUpdateConfigurationWithoutPasswordSet() throws IOException {
		assertFalse(corePlugin.isInternalMonitoringActive());
		try {
			configuration.save("stagemonitor.internal.monitoring", "true", "Transient Configuration Source", null);
			fail();
		} catch (IllegalStateException e) {
			assertEquals("'stagemonitor.password' is not set.", e.getMessage());
		}
		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfiguration() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		assertFalse(corePlugin.isInternalMonitoringActive());

		configuration.save("stagemonitor.internal.monitoring", "true", "Transient Configuration Source", null);

		assertTrue(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationWrongDatatype() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, "").add("stagemonitor.internal.monitoring", "1"));
		configuration.reloadAllConfigurationOptions();
		assertFalse(corePlugin.isInternalMonitoringActive());

		assertEquals("Error in Test Configuration Source: Can't convert '1' to Boolean.", configuration.getConfigurationOptionByKey("stagemonitor.internal.monitoring").getErrorMessage());

		configuration.save("stagemonitor.internal.monitoring", "true", "Test Configuration Source", null);
		assertTrue(corePlugin.isInternalMonitoringActive());

		assertNull(configuration.getConfigurationOptionByKey("stagemonitor.internal.monitoring").getErrorMessage());

	}

	@Test
	public void testUpdateConfigurationWrongConfigurationSource() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		assertFalse(corePlugin.isInternalMonitoringActive());
		try {
			configuration.save("stagemonitor.internal.monitoring", "true", "foo", null);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Configuration source 'foo' does not exist.", e.getMessage());
		}
		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationNotSaveableConfigurationSource() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		configuration.addConfigurationSource(new SystemPropertyConfigurationSource());
		assertFalse(corePlugin.isInternalMonitoringActive());
		try {
			configuration.save("stagemonitor.internal.monitoring", "true", "Java System Properties", null);
			fail();
		} catch (UnsupportedOperationException e) {
			assertEquals("Saving to Java System Properties is not possible.", e.getMessage());
		}
		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationNonDynamicTransient() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		assertEquals(0, corePlugin.getConsoleReportingInterval());
		try {
			configuration.save("stagemonitor.reporting.interval.console", "1", "Transient Configuration Source", null);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Non dynamic options can't be saved to a transient configuration source.", e.getMessage());
		}
		assertEquals(0, corePlugin.getConsoleReportingInterval());
	}

	@Test
	public void testUpdateConfigurationNonDynamicPersistent() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		final ConfigurationSource persistentSourceMock = mock(ConfigurationSource.class);
		when(persistentSourceMock.isSavingPossible()).thenReturn(true);
		when(persistentSourceMock.isSavingPersistent()).thenReturn(true);
		when(persistentSourceMock.getName()).thenReturn("Test Persistent");
		configuration.addConfigurationSource(persistentSourceMock);
		assertEquals(0, corePlugin.getConsoleReportingInterval());

		configuration.save("stagemonitor.reporting.interval.console", "1", "Test Persistent", null);

		verify(persistentSourceMock).save("stagemonitor.reporting.interval.console", "1");
	}

	@Test
	public void testSetNewPasswordViaQueryParamsShouldFail() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		try {
			configuration.save(STAGEMONITOR_PASSWORD, "pwd", "Transient Configuration Source", null);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Config key 'stagemonitor.password' does not exist.", e.getMessage());
		}
	}

	@Test
	public void testUpdateConfigurationWithoutPassword() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, "pwd"));
		assertFalse(corePlugin.isInternalMonitoringActive());

		try {
			configuration.save("stagemonitor.internal.monitoring", "true", "Transient Configuration Source", null);
			fail();
		} catch (IllegalStateException e) {
			assertEquals("Wrong password for 'stagemonitor.password'.", e.getMessage());
		}

		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationWithPassword() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, "pwd"));
		assertFalse(corePlugin.isInternalMonitoringActive());

		configuration.save("stagemonitor.internal.monitoring", "true", "Transient Configuration Source", "pwd");

		assertTrue(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testAddConfigurationSource() {
		Configuration configuration = new Configuration("");
		configuration.addConfigurationSource(new SimpleSource());

		assertEquals(Collections.singletonMap("Transient Configuration Source", true), configuration.getNamesOfConfigurationSources());
	}

	@Test
	public void testIsPasswordSetTrue() throws Exception {
		Configuration configuration = new Configuration("pwd");
		configuration.addConfigurationSource(SimpleSource.forTest("pwd", ""));
		assertTrue(configuration.isPasswordSet());
	}

	@Test
	public void testIsPasswordSetFalse() throws Exception {
		Configuration configuration = new Configuration("pwd");
		assertFalse(configuration.isPasswordSet());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDuplicateLabel() throws Exception {
		final ConfigurationOptionProvider optionProvider = TestConfigurationOptionProvider.of(
				ConfigurationOption.stringOption().key("foo").description("Foo").build(),
				ConfigurationOption.stringOption().key("foo").label("Bar").build());
		new Configuration(Collections.singletonList(optionProvider), Collections.emptyList(), "");
	}

	@Test
	public void testOnConfigurationChanged() throws Exception {
		AtomicBoolean changeListenerFired = new AtomicBoolean(false);
		final ConfigurationOption<String> configurationOption = ConfigurationOption.stringOption()
				.key("foo")
				.dynamic(true)
				.defaultValue("old")
				.addChangeListener(new ConfigurationOption.ChangeListener<String>() {
					@Override
					public void onChange(ConfigurationOption<String> configurationOption, String oldValue, String newValue) {
						assertEquals("foo", configurationOption.getKey());
						assertEquals("old", oldValue);
						assertEquals("new", newValue);
						changeListenerFired.set(true);
						throw new RuntimeException("This is an expected test exception. " +
								"It is thrown to test whether Configuration can cope with change listeners that throw an exception.");
					}
				}).build();

		final ConfigurationOptionProvider optionProvider = TestConfigurationOptionProvider.of(configurationOption);
		final SimpleSource configurationSource = new SimpleSource("test");
		final Configuration config = new Configuration(Collections.singletonList(optionProvider), Collections.singletonList(configurationSource), "");
		config.save("foo", "new", "test");
		assertTrue(changeListenerFired.get());
	}

	@Test(expected = IllegalStateException.class)
	public void testFailOnRequiredValueMissing() throws Exception {
		final ConfigurationOption<String> configurationOption = ConfigurationOption.stringOption().key("foo").required().build();

		final ConfigurationOptionProvider optionProvider = TestConfigurationOptionProvider.of(configurationOption);
		new Configuration(Collections.singletonList(optionProvider), Collections.emptyList(), "", true);
	}

	@Test
	public void testValidateConfigurationOption() throws Exception {
		final ConfigurationOption<Boolean> configurationOption = ConfigurationOption.booleanOption()
				.key("foo")
				.defaultValue(true)
				.addValidator(new ConfigurationOption.Validator<Boolean>() {
					@Override
					public void assertValid(Boolean value) {
						if (!value) {
							throw new IllegalArgumentException("Validation failed");
						}
					}
				})
				.build();

		final ConfigurationOptionProvider optionProvider = TestConfigurationOptionProvider.of(configurationOption);
		final SimpleSource configurationSource = new SimpleSource("test");
		final Configuration config = new Configuration(Collections.singletonList(optionProvider), Collections.singletonList(configurationSource), "");
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
					.defaultValue(false)
					.addValidator(new ConfigurationOption.Validator<Boolean>() {
						@Override
						public void assertValid(Boolean value) {
							if (!value) {
								throw new IllegalArgumentException("Validation failed");
							}
						}
					})
					.build();
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Validation failed", e.getMessage());
		}
	}

	@Test
	public void testToJsonDoesNotThrowException() throws Exception {
		final ConfigurationOptionProvider optionProvider = TestConfigurationOptionProvider.of(
				ConfigurationOption.stringOption().key("foo").description("Foo").configurationCategory("Foos").build());
		final Configuration configuration = new Configuration(Collections.singletonList(optionProvider), Collections.emptyList(), "");
		JsonUtils.getMapper().writeValueAsString(configuration.getConfigurationOptionsByCategory());
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
