package org.stagemonitor.collector.core;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfigurationTest {

	private Configuration configuration = new Configuration();

	@Test
	public void testInvalidPatterns() {
		assertTrue(configuration.getPatternMap("invalidPatternMap", "").isEmpty());
	}

	@Test
	public void testInvalidPatternSyntax() {
		assertTrue(configuration.getPatterns("invalidPatternSyntax", "").isEmpty());
	}

	@Test
	public void testGetInt() {
		assertEquals(2, configuration.getInt("long", 0));
	}

	@Test
	public void testGetLong() {
		assertEquals(2L, configuration.getLong("long", 0L));
	}

	@Test
	public void testGetInvalidLong() {
		assertEquals(2L, configuration.getLong("invalidLong", 2L));
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
		assertEquals(Collections.singletonList("foobar"), configuration.getLowerStrings("string", null));
	}

	@Test
	public void testGetStringsNotExistent() {
		assertEquals(Collections.emptyList(), configuration.getStrings("testGetStringsNotExistent1", null));
		assertEquals(Arrays.asList("Foo", "Bar"), configuration.getStrings("testGetStringsNotExistent2", "Foo , Bar"));
		assertEquals(Collections.emptyList(), configuration.getLowerStrings("testGetStringsNotExistent3", null));
		assertEquals(Arrays.asList("foo", "bar"), configuration.getLowerStrings("testGetStringsNotExistent4", "Foo , Bar"));
	}

	@Test
	public void testCaching() {
		assertEquals("testCaching", configuration.getString("testCaching", "testCaching"));
		assertEquals("testCaching", configuration.getString("testCaching", "testCaching2"));
	}

	@Test
	public void testGetBoolean() {
		assertTrue(configuration.getBoolean("boolean.true", false));
		assertFalse(configuration.getBoolean("boolean.false", true));
		assertTrue(configuration.getBoolean("nonExistent", true));
	}

	@Test
	public void testDefaultValues() {
		assertEquals(0, configuration.getNoOfWarmupRequests());
		assertEquals(0, configuration.getWarmupSeconds());
		assertEquals(true, configuration.isCollectRequestStats());
		assertEquals(false, configuration.isCollectCpuTime());
		assertEquals(false, configuration.isMonitorOnlySpringMvcRequests());
		assertEquals(true, configuration.isCollectHeaders());
		assertEquals(Arrays.asList("cookie"), configuration.getExcludedHeaders());
		final List<Pattern> confidentialQueryParams = configuration.getConfidentialQueryParams();
		final List<String> confidentialQueryParamsAsString = new ArrayList<String>(confidentialQueryParams.size());
		for (Pattern confidentialQueryParam : confidentialQueryParams) {
			confidentialQueryParamsAsString.add(confidentialQueryParam.toString());
		}
		assertEquals(Arrays.asList("(?i).*pass.*", "(?i).*credit.*", "(?i).*pwd.*"), confidentialQueryParamsAsString);
		assertEquals(60L, configuration.getConsoleReportingInterval());
		assertEquals(true, configuration.reportToJMX());
		assertEquals(60, configuration.getGraphiteReportingInterval());
		assertEquals(null, configuration.getGraphiteHostName());
		assertEquals(2003, configuration.getGraphitePort());
		assertEquals(100000L, configuration.getMinExecutionTimeNanos());
		assertEquals(1, configuration.getCallStackEveryXRequestsToGroup());
		assertEquals(true, configuration.isLogCallStacks());
		assertEquals("1w", configuration.getCallStacksTimeToLive());
		assertEquals(null, configuration.getApplicationName());
		assertEquals(null, configuration.getInstanceName());
		assertEquals(null, configuration.getServerUrl());
		assertEquals(Collections.emptyList(), configuration.getExcludedMetricsPatterns());

		final Map<Pattern,String> groupUrls = configuration.getGroupUrls();
		final Map<String, String> groupUrlsAsString = new HashMap<String, String>();
		for (Map.Entry<Pattern, String> entry : groupUrls.entrySet()) {
			groupUrlsAsString.put(entry.getKey().pattern(), entry.getValue());
		}

		final Map<String, String> expectedMap = new HashMap<String, String>();
		expectedMap.put("/\\d+", "/{id}");
		expectedMap.put("(.*).js", "*.js");
		expectedMap.put("(.*).css", "*.css");
		expectedMap.put("(.*).jpg", "*.jpg");
		expectedMap.put("(.*).jpeg", "*.jpeg");
		expectedMap.put("(.*).png", "*.png");

		assertEquals(expectedMap, groupUrlsAsString);
	}
}
