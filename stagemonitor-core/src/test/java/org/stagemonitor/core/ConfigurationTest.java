package org.stagemonitor.core;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfigurationTest {

	private Configuration configuration = new Configuration();

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
		assertEquals(2L, configuration.getLong("invalidLong"));
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
	public void testGetStringsNotExistent() {
		assertEquals(Collections.<String>emptySet(), configuration.getStrings("testGetStringsNotExistent1"));
		assertEquals(new LinkedHashSet<String>(Arrays.asList("Foo", "Bar")), configuration.getStrings("testGetStringsNotExistent2"));
		assertEquals(Collections.<String>emptySet(), configuration.getLowerStrings("testGetStringsNotExistent3"));
		assertEquals(new LinkedHashSet<String>(Arrays.asList("foo", "bar")), configuration.getLowerStrings("testGetStringsNotExistent4"));
	}

	@Test
	public void testCaching() {
		assertEquals("testCaching", configuration.getString("testCaching"));
		assertEquals("testCaching", configuration.getString("testCaching2"));
	}

	@Test
	public void testGetBoolean() {
		assertTrue(configuration.getBoolean("boolean.true"));
		assertFalse(configuration.getBoolean("boolean.false"));
		assertTrue(configuration.getBoolean("nonExistent"));
	}

	@Test
	public void testElasticsearchUrlTrailingSlash() {
		assertEquals("foo", configuration.getElasticsearchUrl());
	}

//	@Test
//	public void testDefaultValues() {
//		assertEquals(0, configuration.getNoOfWarmupRequests());
//		assertEquals(0, configuration.getWarmupSeconds());
//		assertEquals(true, configuration.isCollectRequestStats());
//		assertEquals(false, configuration.isCollectCpuTime());
//		assertEquals(false, configuration.isMonitorOnlySpringMvcRequests());
//		assertEquals(true, configuration.isCollectHeaders());
//		assertEquals(new LinkedHashSet<String>(Arrays.asList("cookie", "authorization")), configuration.getExcludedHeaders());
//		final Collection<Pattern> confidentialQueryParams = configuration.getConfidentialRequestParams();
//		final List<String> confidentialQueryParamsAsString = new ArrayList<String>(confidentialQueryParams.size());
//		for (Pattern confidentialQueryParam : confidentialQueryParams) {
//			confidentialQueryParamsAsString.add(confidentialQueryParam.toString());
//		}
//		assertEquals(Arrays.asList("(?i).*pass.*", "(?i).*credit.*", "(?i).*pwd.*"), confidentialQueryParamsAsString);
//		assertEquals(60L, configuration.getConsoleReportingInterval());
//		assertEquals(true, configuration.reportToJMX());
//		assertEquals(60, configuration.getGraphiteReportingInterval());
//		assertEquals(null, configuration.getGraphiteHostName());
//		assertEquals(2003, configuration.getGraphitePort());
//		assertEquals(100000L, configuration.getMinExecutionTimeNanos());
//		assertEquals(1, configuration.getCallStackEveryXRequestsToGroup());
//		assertEquals(true, configuration.isLogCallStacks());
//		assertEquals("1w", configuration.getCallStacksTimeToLive());
//		assertEquals(null, configuration.getApplicationName());
//		assertEquals(null, configuration.getInstanceName());
//		assertEquals(Collections.<Pattern>emptySet(), configuration.getExcludedMetricsPatterns());
//
//		final Map<Pattern, String> groupUrls = configuration.getGroupUrls();
//		final Map<String, String> groupUrlsAsString = new HashMap<String, String>();
//		for (Map.Entry<Pattern, String> entry : groupUrls.entrySet()) {
//			groupUrlsAsString.put(entry.getKey().pattern(), entry.getValue());
//		}
//
//		final Map<String, String> expectedMap = new HashMap<String, String>();
//		expectedMap.put("(.*).js$", "*.js");
//		expectedMap.put("(.*).css$", "*.css");
//		expectedMap.put("(.*).jpg$", "*.jpg");
//		expectedMap.put("(.*).jpeg$", "*.jpeg");
//		expectedMap.put("(.*).png$", "*.png");
//
//		assertEquals(expectedMap, groupUrlsAsString);
//	}
}
