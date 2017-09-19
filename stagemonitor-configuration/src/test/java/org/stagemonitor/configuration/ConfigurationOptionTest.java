package org.stagemonitor.configuration;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.configuration.source.ConfigurationSource;
import org.stagemonitor.configuration.source.SimpleSource;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConfigurationOptionTest {

	private final ConfigurationOption<Map<Pattern, String>> invalidPatternMap = ConfigurationOption.regexMapOption().key("invalidPatternMap").build();
	private final ConfigurationOption<Collection<Pattern>> invalidPatternSyntax = ConfigurationOption.regexListOption().key("invalidPatternSyntax").build();
	private final ConfigurationOption<Long> aLong = ConfigurationOption.longOption().key("long").build();
	private final ConfigurationOption<Long> invalidLong = ConfigurationOption.longOption().key("invalidLong").buildWithDefault(2L);
	private final ConfigurationOption<String> string = ConfigurationOption.stringOption().key("string").buildRequired();
	private final ConfigurationOption<Collection<String>> lowerStrings = ConfigurationOption.lowerStringsOption().key("lowerStrings").build();
	private final ConfigurationOption<Collection<String>> strings = ConfigurationOption.stringsOption().key("strings").build();
	private final ConfigurationOption<Boolean> booleanTrue = ConfigurationOption.booleanOption().key("boolean.true").build();
	private final ConfigurationOption<Boolean> booleanFalse = ConfigurationOption.booleanOption().key("boolean.false").build();
	private final ConfigurationOption<Boolean> booleanInvalid = ConfigurationOption.booleanOption().key("boolean.invalid").build();
	private final ConfigurationOption<String> testCaching = ConfigurationOption.stringOption().key("testCaching").build();
	private final ConfigurationOption<String> testUpdate = ConfigurationOption.stringOption().key("testUpdate").dynamic(true).build();
	private final ConfigurationOption<Optional<String>> testOptionalWithValue = ConfigurationOption.stringOption().key("testOptionalWithValue").dynamic(true).buildOptional();
	private final ConfigurationOption<Optional<String>> testOptionalWithoutValue = ConfigurationOption.stringOption().key("testOptionalWithoutValue").dynamic(true).buildOptional();
	private ConfigurationOption<String> testAlternateKeys = ConfigurationOption.stringOption()
			.key("primaryKey")
			.aliasKeys("alternateKey1", "alternateKey2")
			.dynamic(true)
			.build();
	private ConfigurationRegistry configuration;
	private SimpleSource configSource = SimpleSource
			.forTest("invalidLong", "two")
			.add("stagemonitor.reporting.elasticsearch.url", "foo/")
			.add("invalidPatternMap", "(.*).js: *.js (.*).css:  *.css")
			.add("invalidPatternSyntax", "(.*.js")
			.add("long", "2")
			.add("string", "fooBar")
			.add("lowerStrings", "fooBar")
			.add("strings", "fooBar , barFoo")
			.add("boolean.true", "true")
			.add("boolean.false", "false")
			.add("boolean.invalid", "ture")
			.add("testCaching", "testCaching")
			.add("testOptionalWithValue", "present");


	@Before
	public void before() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		configuration = createConfiguration(Arrays.asList(invalidPatternMap, invalidPatternSyntax, aLong, invalidLong, string,
				lowerStrings, strings, booleanTrue, booleanFalse, booleanInvalid, testCaching, testUpdate,
				testOptionalWithValue, testOptionalWithoutValue), configSource);
	}

	private ConfigurationRegistry createConfiguration(List<ConfigurationOption<?>> configurationOptions, ConfigurationSource configurationSource) {
		final List<ConfigurationOptionProvider> configurationOptionProviders = Collections.singletonList(new ConfigurationOptionProvider() {
			public List<ConfigurationOption<?>> getConfigurationOptions() {
				return configurationOptions;
			}
		});
		return new ConfigurationRegistry(configurationOptionProviders, Collections.singletonList(configurationSource), null);
	}

	@Test
	public void testInvalidPatterns() {
		assertTrue(invalidPatternMap.getValue().isEmpty());
	}

	@Test
	public void testInvalidPatternSyntax() {
		assertTrue(invalidPatternSyntax.getValue().isEmpty());
	}

	@Test
	public void testGetInt() {
		assertEquals(Long.valueOf(2L), aLong.getValue());
	}

	@Test
	public void testGetInvalidLong() {
		assertEquals(Long.valueOf(2L), invalidLong.getValue());
	}

	@Test
	public void testGetString() {
		assertEquals("fooBar", string.getValue());
	}

	@Test
	public void testGetLowerStrings() {
		assertEquals(Collections.singleton("foobar"), lowerStrings.getValue());
	}

	@Test
	public void testCachingAndReload() {
		assertEquals("testCaching", testCaching.getValue());
		configSource.add("testCaching", "testCaching2");
		assertEquals("testCaching", testCaching.getValue());
		configuration.reloadDynamicConfigurationOptions();
		assertEquals("testCaching", testCaching.getValue());
		configuration.reloadAllConfigurationOptions();
		assertEquals("testCaching2", testCaching.getValue());
	}

	@Test
	public void testGetBoolean() {
		assertTrue(booleanTrue.getValue());
		assertFalse(booleanFalse.getValue());
	}

	@Test
	public void testUpdate() throws IOException {
		assertNull(testUpdate.getValue());
		testUpdate.update("updated!", "Test Configuration Source");
		assertEquals("updated!", testUpdate.getValue());
	}

	@Test
	public void testAlternateKeysPrimary() {
		final ConfigurationRegistry configuration = createConfiguration(Collections.singletonList(testAlternateKeys), SimpleSource.forTest("primaryKey", "foo"));

		assertEquals("foo", configuration.getConfigurationOptionByKey("primaryKey").getValueAsString());
		assertEquals("foo", configuration.getConfigurationOptionByKey("alternateKey1").getValueAsString());
		assertEquals("foo", configuration.getConfigurationOptionByKey("alternateKey2").getValueAsString());
	}

	@Test
	public void testAlternateKeysAlternate() {
		final ConfigurationRegistry configuration = createConfiguration(Collections.singletonList(testAlternateKeys), SimpleSource.forTest("alternateKey1", "foo"));

		assertEquals("foo", configuration.getConfigurationOptionByKey("primaryKey").getValueAsString());
		assertEquals("foo", configuration.getConfigurationOptionByKey("alternateKey1").getValueAsString());
		assertEquals("foo", configuration.getConfigurationOptionByKey("alternateKey2").getValueAsString());
	}

	@Test
	public void testAlternateKeysPrimaryAndAlternate() {
		final ConfigurationRegistry configuration = createConfiguration(Collections.singletonList(testAlternateKeys), SimpleSource.forTest("primaryKey", "foo").add("alternateKey1", "bar"));

		assertEquals("foo", configuration.getConfigurationOptionByKey("primaryKey").getValueAsString());
		assertEquals("foo", configuration.getConfigurationOptionByKey("alternateKey1").getValueAsString());
		assertEquals("foo", configuration.getConfigurationOptionByKey("alternateKey2").getValueAsString());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDuplicateAlternateKeys() {
		createConfiguration(Arrays.asList(
				ConfigurationOption.stringOption().key("primaryKey1").aliasKeys("alternateKey1").build(),
				ConfigurationOption.stringOption().key("primaryKey2").aliasKeys("alternateKey1").build()
		), new SimpleSource());
	}

	@Test
	public void testOptional() {
		assertEquals("present", testOptionalWithValue.getValue().get());
		assertTrue(testOptionalWithValue.getValue().isPresent());
		assertFalse(testOptionalWithoutValue.getValue().isPresent());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDefaultValueNull() {
		ConfigurationOption.stringOption().key("foo").buildWithDefault(null);
	}

	@Test
	public void testWithOptions_valid() {
		final ConfigurationOption<String> option = ConfigurationOption.stringOption()
				.key("test.options")
				.addValidOptions("foo", "bar")
				.buildWithDefault("foo");
		final ConfigurationRegistry configuration = createConfiguration(Collections.singletonList(option), SimpleSource.forTest("test.options", "bar"));
		assertThat(configuration.getConfigurationOptionByKey("test.options").getValueAsString()).isEqualTo("bar");
		assertThatThrownBy(() -> configuration.save("test.options", "baz", "Test Configuration Source"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Invalid option");
	}

	@Test
	public void testWithOptions_invalidDefault() {
		assertThatThrownBy(() -> ConfigurationOption.stringOption()
				.key("test.options")
				.addValidOptions("foo", "bar")
				.buildWithDefault("baz"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Invalid option");
	}

	@Test
	public void testWithOptions_sealedOptions() {
		assertThatThrownBy(() -> ConfigurationOption.stringOption()
				.key("test.options")
				.addValidOptions("foo", "bar")
				.sealValidOptions()
				.addValidOption("baz")
				.buildWithDefault("baz"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Options are sealed, you can't add any new ones");
	}

	interface Strategy {
	}

	public static class DefaultStrategyImpl implements Strategy {
	}

	public static class SpecialStrategyImpl implements Strategy {
	}

	public static class NoMetaInfServicesStrategyImpl implements Strategy {
	}

	@Test
	public void testServiceLoaderStrategyOption() throws Exception {
		final ConfigurationOption<Strategy> option = ConfigurationOption.serviceLoaderStategyOption(Strategy.class)
				.key("test.strategy")
				.dynamic(true)
				.buildWithDefault(new DefaultStrategyImpl());
		final ConfigurationRegistry configuration = createConfiguration(Collections.singletonList(option), new SimpleSource());
		assertThat(option.getValidOptions()).containsExactlyInAnyOrder(DefaultStrategyImpl.class.getName(), SpecialStrategyImpl.class.getName());
		assertThat(option.getValue()).isInstanceOf(DefaultStrategyImpl.class);
		configuration.save("test.strategy", SpecialStrategyImpl.class.getName(), SimpleSource.NAME);
		assertThat(option.getValue()).isInstanceOf(SpecialStrategyImpl.class);
		assertThatThrownBy(() -> configuration.save("test.strategy", NoMetaInfServicesStrategyImpl.class.getName(), SimpleSource.NAME))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Invalid option");
	}

	enum TestEnum {
		FOO, BAR;

		@Override
		public String toString() {
			return super.toString().toLowerCase();
		}
	}

	@Test
	public void testEnum() throws Exception {
		final ConfigurationOption<TestEnum> option = ConfigurationOption.enumOption(TestEnum.class)
				.key("test.enum")
				.dynamic(true)
				.buildWithDefault(TestEnum.FOO);
		final ConfigurationRegistry configuration = createConfiguration(Collections.singletonList(option), new SimpleSource());
		assertThat(option.getValidOptions()).containsExactlyInAnyOrder("FOO", "BAR");
		assertThat(option.getValidOptionsLabelMap()).containsEntry("FOO", "foo").containsEntry("BAR", "bar");
		assertThat(option.getValue()).isEqualTo(TestEnum.FOO);
		configuration.save("test.enum", "BAR", SimpleSource.NAME);
		assertThat(option.getValue()).isEqualTo(TestEnum.BAR);
		assertThatThrownBy(() -> configuration.save("test.enum", "BAZ", SimpleSource.NAME))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testValidOptions_list() throws Exception {
		assertThatThrownBy(() -> ConfigurationOption.integersOption()
				.key("test.list")
				.dynamic(true)
				.addValidOption(Arrays.asList(1, 2))
				.buildWithDefault(Collections.singleton(1)))
				.isInstanceOf(UnsupportedOperationException.class);
	}

}
