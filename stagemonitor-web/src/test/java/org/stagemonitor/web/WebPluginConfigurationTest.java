package org.stagemonitor.web;

import com.codahale.metrics.MetricRegistry;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.stagemonitor.web.WebPlugin.GROUP_URLS;
import static org.stagemonitor.web.WebPlugin.HTTP_COLLECT_HEADERS;
import static org.stagemonitor.web.WebPlugin.HTTP_HEADERS_EXCLUDED;
import static org.stagemonitor.web.WebPlugin.HTTP_REQUESTPARAMS_CONFIDENTIAL_REGEX;


public class WebPluginConfigurationTest {

	private Configuration configuration = new Configuration();

	@Before
	public void before() {
		final WebPlugin webPlugin = new WebPlugin();
		webPlugin.initializePlugin(mock(MetricRegistry.class), configuration);
	}

	@Test
	public void testDefaultValues() {
//		assertEquals(false, configuration.getisMonitorOnlySpringMvcRequests());
		assertEquals(true, configuration.getBoolean(HTTP_COLLECT_HEADERS));
		assertEquals(new LinkedHashSet<String>(Arrays.asList("cookie", "authorization")), configuration.getLowerStrings(HTTP_HEADERS_EXCLUDED));
		final Collection<Pattern> confidentialQueryParams = configuration.getPatterns(HTTP_REQUESTPARAMS_CONFIDENTIAL_REGEX);
		final List<String> confidentialQueryParamsAsString = new ArrayList<String>(confidentialQueryParams.size());
		for (Pattern confidentialQueryParam : confidentialQueryParams) {
			confidentialQueryParamsAsString.add(confidentialQueryParam.toString());
		}
		assertEquals(Arrays.asList("(?i).*pass.*", "(?i).*credit.*", "(?i).*pwd.*"), confidentialQueryParamsAsString);

		final Map<Pattern, String> groupUrls = configuration.getPatternMap(GROUP_URLS);
		final Map<String, String> groupUrlsAsString = new HashMap<String, String>();
		for (Map.Entry<Pattern, String> entry : groupUrls.entrySet()) {
			groupUrlsAsString.put(entry.getKey().pattern(), entry.getValue());
		}

		final Map<String, String> expectedMap = new HashMap<String, String>();
		expectedMap.put("(.*).js$", "*.js");
		expectedMap.put("(.*).css$", "*.css");
		expectedMap.put("(.*).jpg$", "*.jpg");
		expectedMap.put("(.*).jpeg$", "*.jpeg");
		expectedMap.put("(.*).png$", "*.png");

		assertEquals(expectedMap, groupUrlsAsString);
	}
}
