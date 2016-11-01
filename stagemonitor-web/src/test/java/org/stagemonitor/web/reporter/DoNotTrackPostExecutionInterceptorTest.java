package org.stagemonitor.web.reporter;

import com.uber.jaeger.Span;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.reporter.ElasticsearchSpanReporter;
import org.stagemonitor.requestmonitor.reporter.SpanReporter;
import org.stagemonitor.requestmonitor.utils.SpanTags;
import org.stagemonitor.web.WebPlugin;

import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DoNotTrackPostExecutionInterceptorTest {

	private ElasticsearchSpanReporter reporter;
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
		reporter = new ElasticsearchSpanReporter();
		reporter.init(new SpanReporter.InitArguments(configuration, mock(Metric2Registry.class)));
	}

	@Test
	public void testHonorDoNotTrack() throws Exception {
		when(webPlugin.isHonorDoNotTrackHeader()).thenReturn(true);
		final Span span = mock(Span.class);
		when(span.getTags()).thenReturn(Collections.singletonMap(SpanTags.HTTP_HEADERS_PREFIX + "dnt", "1"));

		reporter.report(new SpanReporter.ReportArguments(null, span, null));

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), any());
		Assert.assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(null, span)));
	}

	@Test
	public void testDoNotTrackDisabled() throws Exception {
		when(webPlugin.isHonorDoNotTrackHeader()).thenReturn(true);
		final Span span = mock(Span.class);
		when(span.getTags()).thenReturn(Collections.singletonMap(SpanTags.HTTP_HEADERS_PREFIX + "dnt", "0"));

		reporter.report(new SpanReporter.ReportArguments(null, span, null));

		verify(elasticsearchClient).index(anyString(), anyString(), any());
		Assert.assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(null, span)));
	}

	@Test
	public void testNoDoNotTrackHeader() throws Exception {
		when(webPlugin.isHonorDoNotTrackHeader()).thenReturn(true);
		final Span span = mock(Span.class);
		when(span.getTags()).thenReturn(Collections.emptyMap());

		reporter.report(new SpanReporter.ReportArguments(null, span, null));

		verify(elasticsearchClient).index(anyString(), anyString(), any());
		Assert.assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(null, span)));
	}

	@Test
	public void testDontHonorDoNotTrack() throws Exception {
		when(webPlugin.isHonorDoNotTrackHeader()).thenReturn(false);
		final Span span = mock(Span.class);
		when(span.getTags()).thenReturn(Collections.singletonMap(SpanTags.HTTP_HEADERS_PREFIX + "dnt", "1"));

		reporter.report(new SpanReporter.ReportArguments(null, span, null));

		verify(elasticsearchClient).index(anyString(), anyString(), any());
		Assert.assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(null, span)));
	}

}
