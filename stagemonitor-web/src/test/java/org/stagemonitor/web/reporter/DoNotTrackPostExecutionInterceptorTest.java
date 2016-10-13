package org.stagemonitor.web.reporter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.reporter.ElasticsearchRequestTraceReporter;
import org.stagemonitor.requestmonitor.reporter.SpanReporter;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.HttpRequestTrace;

import java.util.Collections;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DoNotTrackPostExecutionInterceptorTest {

	private ElasticsearchRequestTraceReporter reporter;
	private ElasticsearchClient elasticsearchClient;
	private WebPlugin webPlugin;

	@Before
	public void setUp() throws Exception {
		Configuration configuration = mock(Configuration.class);
		CorePlugin corePlugin = mock(CorePlugin.class);
		RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		this.webPlugin = mock(WebPlugin.class);

		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(configuration.getConfig(WebPlugin.class)).thenReturn(webPlugin);
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(1000000d);
		when(requestMonitorPlugin.getOnlyReportRequestsWithNameToElasticsearch()).thenReturn(Collections.emptyList());
		when(corePlugin.getElasticsearchUrl()).thenReturn("http://localhost:9200");
		when(corePlugin.getElasticsearchUrls()).thenReturn(Collections.singletonList("http://localhost:9200"));
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient = mock(ElasticsearchClient.class));
		when(corePlugin.getMetricRegistry()).thenReturn(new Metric2Registry());
		when(webPlugin.isHonorDoNotTrackHeader()).thenReturn(true);
		reporter = new ElasticsearchRequestTraceReporter();
		reporter.init(new SpanReporter.InitArguments(configuration));
	}

	@Test
	public void testHonorDoNotTrack() throws Exception {
		when(webPlugin.isHonorDoNotTrackHeader()).thenReturn(true);
		final HttpRequestTrace requestTrace = mock(HttpRequestTrace.class);
		when(requestTrace.getHeaders()).thenReturn(Collections.singletonMap("dnt", "1"));

		reporter.report(new SpanReporter.ReportArguments(requestTrace, null));

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), anyObject());
		Assert.assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(requestTrace, null)));
	}

	@Test
	public void testDoNotTrackDisabled() throws Exception {
		when(webPlugin.isHonorDoNotTrackHeader()).thenReturn(true);
		final HttpRequestTrace requestTrace = mock(HttpRequestTrace.class);
		when(requestTrace.getHeaders()).thenReturn(Collections.singletonMap("dnt", "0"));

		reporter.report(new SpanReporter.ReportArguments(requestTrace, null));

		verify(elasticsearchClient).index(anyString(), anyString(), anyObject());
		Assert.assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(requestTrace, null)));
	}

	@Test
	public void testNoDoNotTrackHeader() throws Exception {
		when(webPlugin.isHonorDoNotTrackHeader()).thenReturn(true);
		final HttpRequestTrace requestTrace = mock(HttpRequestTrace.class);
		when(requestTrace.getHeaders()).thenReturn(Collections.emptyMap());

		reporter.report(new SpanReporter.ReportArguments(requestTrace, null));

		verify(elasticsearchClient).index(anyString(), anyString(), anyObject());
		Assert.assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(requestTrace, null)));
	}

	@Test
	public void testDontHonorDoNotTrack() throws Exception {
		when(webPlugin.isHonorDoNotTrackHeader()).thenReturn(false);
		final HttpRequestTrace requestTrace = mock(HttpRequestTrace.class);
		when(requestTrace.getHeaders()).thenReturn(Collections.singletonMap("dnt", "1"));

		reporter.report(new SpanReporter.ReportArguments(requestTrace, null));

		verify(elasticsearchClient).index(anyString(), anyString(), anyObject());
		Assert.assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(requestTrace, null)));
	}

}
