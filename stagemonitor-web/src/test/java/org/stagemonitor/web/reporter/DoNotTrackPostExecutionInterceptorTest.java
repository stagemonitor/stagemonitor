package org.stagemonitor.web.reporter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.MockTracer;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.reporter.ElasticsearchSpanReporter;
import org.stagemonitor.requestmonitor.reporter.SpanReporter;
import org.stagemonitor.requestmonitor.tracing.NoopSpan;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.MonitoredHttpRequest;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

import java.util.Collections;

import javax.servlet.FilterChain;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DoNotTrackPostExecutionInterceptorTest {

	private ElasticsearchSpanReporter reporter;
	private ElasticsearchClient elasticsearchClient;
	private WebPlugin webPlugin;
	private Configuration configuration;

	@Before
	public void setUp() throws Exception {
		configuration = mock(Configuration.class);
		CorePlugin corePlugin = mock(CorePlugin.class);
		RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		this.webPlugin = mock(WebPlugin.class);

		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(configuration.getConfig(WebPlugin.class)).thenReturn(webPlugin);
		when(requestMonitorPlugin.getOnlyReportNSpansPerMinuteTo()).thenReturn(1000000d);
		when(requestMonitorPlugin.getOnlyReportRequestsWithNameToElasticsearch()).thenReturn(Collections.emptyList());
		when(corePlugin.getElasticsearchUrl()).thenReturn("http://localhost:9200");
		when(corePlugin.getElasticsearchUrls()).thenReturn(Collections.singletonList("http://localhost:9200"));
		elasticsearchClient = mock(ElasticsearchClient.class);
		when(elasticsearchClient.isElasticsearchAvailable()).thenReturn(true);
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient);
		when(corePlugin.getMetricRegistry()).thenReturn(new Metric2Registry());
		when(webPlugin.isHonorDoNotTrackHeader()).thenReturn(true);
		reporter = new ElasticsearchSpanReporter();
		reporter.init(new SpanReporter.InitArguments(configuration, mock(Metric2Registry.class)));
		when(requestMonitorPlugin.getTracer()).thenReturn(new MockTracer());
	}

	@Test
	public void testHonorDoNotTrack() throws Exception {
		when(webPlugin.isHonorDoNotTrackHeader()).thenReturn(true);
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("dnt", "1");
		final io.opentracing.Span span = new MonitoredHttpRequest(request, mock(StatusExposingByteCountingServletResponse.class),
				mock(FilterChain.class), configuration).createSpan();

		assertSame(NoopSpan.INSTANCE, span);
	}

	@Test
	public void testDoNotTrackDisabled() throws Exception {
		when(webPlugin.isHonorDoNotTrackHeader()).thenReturn(true);
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("dnt", "0");
		final io.opentracing.Span span = new MonitoredHttpRequest(request, mock(StatusExposingByteCountingServletResponse.class),
				mock(FilterChain.class), configuration).createSpan();

		assertNotSame(NoopSpan.INSTANCE, span);
	}

	@Test
	public void testNoDoNotTrackHeader() throws Exception {
		when(webPlugin.isHonorDoNotTrackHeader()).thenReturn(true);
		final MockHttpServletRequest request = new MockHttpServletRequest();
		final io.opentracing.Span span = new MonitoredHttpRequest(request, mock(StatusExposingByteCountingServletResponse.class),
				mock(FilterChain.class), configuration).createSpan();

		assertNotSame(NoopSpan.INSTANCE, span);
	}

	@Test
	public void testDontHonorDoNotTrack() throws Exception {
		when(webPlugin.isHonorDoNotTrackHeader()).thenReturn(false);
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("dnt", "1");
		final io.opentracing.Span span = new MonitoredHttpRequest(request, mock(StatusExposingByteCountingServletResponse.class),
				mock(FilterChain.class), configuration).createSpan();

		assertNotSame(NoopSpan.INSTANCE, span);
	}

}
