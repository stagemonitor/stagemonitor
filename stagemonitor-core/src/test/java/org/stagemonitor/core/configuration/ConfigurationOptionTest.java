package org.stagemonitor.core.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.source.SimpleSource;

public class ConfigurationOptionTest {

	private final ConfigurationOption<Map<Pattern,String>> invalidPatternMap = ConfigurationOption.regexMapOption().key("invalidPatternMap").build();
	private final ConfigurationOption<Collection<Pattern>> invalidPatternSyntax = ConfigurationOption.regexListOption().key("invalidPatternSyntax").build();
	private final ConfigurationOption<Long> aLong = ConfigurationOption.longOption().key("long").build();
	private final ConfigurationOption<Long> invalidLong = ConfigurationOption.longOption().key("invalidLong").defaultValue(2L).build();
	private final ConfigurationOption<String> string = ConfigurationOption.stringOption().key("string").build();
	private final ConfigurationOption<Collection<String>> lowerStrings = ConfigurationOption.lowerStringsOption().key("lowerStrings").build();
	private final ConfigurationOption<Collection<String>> strings = ConfigurationOption.stringsOption().key("strings").build();
	private final ConfigurationOption<Boolean> booleanTrue = ConfigurationOption.booleanOption().key("boolean.true").build();
	private final ConfigurationOption<Boolean> booleanFalse = ConfigurationOption.booleanOption().key("boolean.false").build();
	private final ConfigurationOption<Boolean> booleanInvalid = ConfigurationOption.booleanOption().key("boolean.invalid").build();
	private final ConfigurationOption<String> testCaching = ConfigurationOption.stringOption().key("testCaching").build();
	private final ConfigurationOption<String> testUpdate = ConfigurationOption.stringOption().key("testUpdate").dynamic(true).build();
	private Configuration configuration = new Configuration(StagemonitorPlugin.class);
	private CorePlugin corePlugin;
	private SimpleSource configSource = SimpleSource
			.forTest("invalidLong", "two")
			.add("stagemonitor.elasticsearch.url", "foo/")
			.add("invalidPatternMap", "(.*).js: *.js (.*).css:  *.css")
			.add("invalidPatternSyntax", "(.*.js")
			.add("long", "2")
			.add("string", "fooBar")
			.add("lowerStrings", "fooBar")
			.add("strings", "fooBar , barFoo")
			.add("boolean.true", "true")
			.add("boolean.false", "false")
			.add("boolean.invalid", "ture")
			.add("testCaching", "testCaching");

	@Before
	public void before() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		corePlugin = configuration.getConfig(CorePlugin.class);
		configuration.addConfigurationSource(configSource);
		configuration.reloadDynamicConfigurationOptions();

		Method registerPluginConfiguration = Configuration.class.getDeclaredMethod("registerOptionProvider", ConfigurationOptionProvider.class);
		registerPluginConfiguration.setAccessible(true);
		registerPluginConfiguration.invoke(configuration, new StagemonitorPlugin() {
			public List<ConfigurationOption<?>> getConfigurationOptions() {
				return Arrays.<ConfigurationOption<?>>asList(invalidPatternMap, invalidPatternSyntax, aLong, invalidLong, string,
						lowerStrings, strings, booleanTrue, booleanFalse, booleanInvalid, testCaching, testUpdate);
			}
		});
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
	public void testElasticsearchUrlTrailingSlash() {
		assertEquals("foo", corePlugin.getElasticsearchUrl());
	}

	@Test
	public void testDefaultValues() {
		assertEquals(0L, corePlugin.getConsoleReportingInterval());
		assertEquals(true, corePlugin.reportToJMX());
		assertEquals(60, corePlugin.getGraphiteReportingInterval());
		assertEquals(null, corePlugin.getGraphiteHostName());
		assertEquals(2003, corePlugin.getGraphitePort());
		assertEquals(null, corePlugin.getApplicationName());
		assertEquals(null, corePlugin.getInstanceName());
		assertEquals(Collections.<Pattern>emptyList(), corePlugin.getExcludedMetricsPatterns());
	}

	@Test
	public void testUpdate() throws IOException {
		assertNull(testUpdate.getValue());
		testUpdate.update("updated!", "Test Configuration Source");
		assertEquals("updated!", testUpdate.getValue());
	}
}
