package org.stagemonitor.requestmonitor;

import com.codahale.metrics.MetricRegistry;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.Configuration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.stagemonitor.requestmonitor.RequestMonitorPlugin.CALL_STACK_EVERY_XREQUESTS_TO_GROUP;
import static org.stagemonitor.requestmonitor.RequestMonitorPlugin.COLLECT_REQUEST_STATS;
import static org.stagemonitor.requestmonitor.RequestMonitorPlugin.CPU_TIME;
import static org.stagemonitor.requestmonitor.RequestMonitorPlugin.LOG_CALL_STACKS;
import static org.stagemonitor.requestmonitor.RequestMonitorPlugin.NO_OF_WARMUP_REQUESTS;
import static org.stagemonitor.requestmonitor.RequestMonitorPlugin.PROFILER_MIN_EXECUTION_TIME_NANOS;
import static org.stagemonitor.requestmonitor.RequestMonitorPlugin.REQUEST_TRACE_TTL;
import static org.stagemonitor.requestmonitor.RequestMonitorPlugin.WARMUP_SECONDS;

public class RequestMonitorPluginConfigurationTest {

	private Configuration configuration = new Configuration();

	@Before
	public void before() {
		final RequestMonitorPlugin requestMonitorPlugin = new RequestMonitorPlugin();
		requestMonitorPlugin.initializePlugin(mock(MetricRegistry.class), configuration);
	}

	@Test
	public void testDefaultValues() {
		assertEquals(0, configuration.getInt(NO_OF_WARMUP_REQUESTS));
		assertEquals(0, configuration.getInt(WARMUP_SECONDS));
		assertEquals(true, configuration.getBoolean(COLLECT_REQUEST_STATS));
		assertEquals(false, configuration.getBoolean(CPU_TIME));
//		assertEquals(false, configuration.isMonitorOnlySpringMvcRequests());
//		assertEquals(true, configuration.isCollectHeaders());
//		assertEquals(new LinkedHashSet<String>(Arrays.asList("cookie", "authorization")), configuration.getExcludedHeaders());
//		final Collection<Pattern> confidentialQueryParams = configuration.getConfidentialRequestParams();
//		final List<String> confidentialQueryParamsAsString = new ArrayList<String>(confidentialQueryParams.size());
//		for (Pattern confidentialQueryParam : confidentialQueryParams) {
//			confidentialQueryParamsAsString.add(confidentialQueryParam.toString());
//		}
//		assertEquals(Arrays.asList("(?i).*pass.*", "(?i).*credit.*", "(?i).*pwd.*"), confidentialQueryParamsAsString);

		assertEquals(100000L, configuration.getLong(PROFILER_MIN_EXECUTION_TIME_NANOS));
		assertEquals(1, configuration.getInt(CALL_STACK_EVERY_XREQUESTS_TO_GROUP));
		assertEquals(true, configuration.getBoolean(LOG_CALL_STACKS));
		assertEquals("1w", configuration.getString(REQUEST_TRACE_TTL));

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
	}
}
