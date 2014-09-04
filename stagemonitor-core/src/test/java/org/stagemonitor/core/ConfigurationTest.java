package org.stagemonitor.core;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfigurationTest {

	private Configuration configuration = new Configuration();

	private Map<String, String> config = new HashMap<String, String>() {{
		put("invalidLong", "two");
		put("stagemonitor.elasticsearch.url", "foo/");
	}};

	@Before
	public void before() {
		configuration.addConfigurationSource(new ConfigurationSource() {
			public String getValue(String key) {
				return config.get(key);
			}
			public void reload() {
			}
		}, true);

		configuration.add(ConfigurationOption.builder().key("invalidPatternMap").defaultValue("(.*).js: *.js (.*).css:  *.css").build());
		configuration.add(ConfigurationOption.builder().key("invalidPatternSyntax").defaultValue("(.*.js").build());
		configuration.add(ConfigurationOption.builder().key("long").defaultValue("2").build());
		configuration.add(ConfigurationOption.builder().key("invalidLong").defaultValue("2").build());
		configuration.add(ConfigurationOption.builder().key("invalidLongDefault").defaultValue("two").build());
		configuration.add(ConfigurationOption.builder().key("string").defaultValue("fooBar").build());
		configuration.add(ConfigurationOption.builder().key("strings").defaultValue("fooBar , barFoo").build());
		configuration.add(ConfigurationOption.builder().key("boolean.true").defaultValue("true").build());
		configuration.add(ConfigurationOption.builder().key("boolean.false").defaultValue("false").build());
		configuration.add(ConfigurationOption.builder().key("boolean.invalid").defaultValue("ture").build());
		configuration.add(ConfigurationOption.builder().key("testCaching").defaultValue("testCaching").build());
	}

	@Test
	public void testInvalidPatterns() {
		assertTrue(configuration.getPatternMap("invalidPatternMap").isEmpty());
	}

	@Test
	public void testInvalidPatternSyntax() {
		assertTrue(configuration.getPatterns("invalidPatternSyntax").isEmpty());
	}

	@Test
	public void testGetInt() {
		assertEquals(2, configuration.getInt("long"));
	}

	@Test
	public void testGetLong() {
		assertEquals(2L, configuration.getLong("long"));
	}

	@Test
	public void testGetInvalidLong() {
		assertEquals(2, configuration.getLong("invalidLong"));
		assertEquals(-1, configuration.getLong("invalidLongDefault"));
	}

	@Test
	public void testGetString() {
		assertEquals("fooBar", configuration.getString("string"));
	}

	@Test
	public void testGetStringNotExistent() {
		assertEquals(null, configuration.getString("testGetStringNotExistent"));
	}

	@Test
	public void testGetLowerStrings() {
		assertEquals(Collections.singleton("foobar"), configuration.getLowerStrings("string"));
	}

	@Test
	public void testCachingAndReload() {
		assertEquals("testCaching", configuration.getString("testCaching"));
		config.put("testCaching", "testCaching2");
		assertEquals("testCaching", configuration.getString("testCaching"));
		configuration.reload();
		assertEquals("testCaching2", configuration.getString("testCaching"));
	}

	@Test
	public void testGetBoolean() {
		assertTrue(configuration.getBoolean("boolean.true"));
		assertFalse(configuration.getBoolean("boolean.false"));
		assertFalse(configuration.getBoolean("nonExistent"));
	}

	@Test
	public void testElasticsearchUrlTrailingSlash() {
		assertEquals("foo", configuration.getElasticsearchUrl());
	}

	@Test
	public void testDefaultValues() {
		assertEquals(60L, configuration.getConsoleReportingInterval());
		assertEquals(true, configuration.reportToJMX());
		assertEquals(60, configuration.getGraphiteReportingInterval());
		assertEquals(null, configuration.getGraphiteHostName());
		assertEquals(2003, configuration.getGraphitePort());
		assertEquals(null, configuration.getApplicationName());
		assertEquals(null, configuration.getInstanceName());
		assertEquals(Collections.<Pattern>emptySet(), configuration.getExcludedMetricsPatterns());
	}
}
