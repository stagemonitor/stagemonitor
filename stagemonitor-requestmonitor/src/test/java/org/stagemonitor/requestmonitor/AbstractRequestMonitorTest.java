package org.stagemonitor.requestmonitor;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.uber.jaeger.context.TracingUtils;

import org.junit.After;
import org.junit.Before;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Filter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.requestmonitor.reporter.ReportingSpanEventListener;
import org.stagemonitor.requestmonitor.sampling.SamplePriorityDeterminingSpanEventListener;
import org.stagemonitor.requestmonitor.tracing.NoopTracer;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrappingTracer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.opentracing.Tracer;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractRequestMonitorTest {

	protected CorePlugin corePlugin;
	protected RequestMonitorPlugin requestMonitorPlugin;
	protected Metric2Registry registry;
	protected RequestMonitor requestMonitor;
	protected Configuration configuration;
	protected Map<String, Object> tags = new HashMap<>();
	protected SamplePriorityDeterminingSpanEventListener samplePriorityDeterminingSpanInterceptor;
	protected SpanWrappingTracer tracer;
	protected SpanCapturingReporter spanCapturingReporter;

	@Before
	public void before() {
		requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		configuration = mock(Configuration.class);
		corePlugin = mock(CorePlugin.class);
		registry = mock(Metric2Registry.class);

		doReturn(corePlugin).when(configuration).getConfig(CorePlugin.class);
		doReturn(requestMonitorPlugin).when(configuration).getConfig(RequestMonitorPlugin.class);

		doReturn(true).when(corePlugin).isStagemonitorActive();
		doReturn(1000).when(corePlugin).getThreadPoolQueueCapacityLimit();
		doReturn(new Metric2Registry()).when(corePlugin).getMetricRegistry();
		doReturn(Collections.singletonList("http://mockhost:9200")).when(corePlugin).getElasticsearchUrls();
		ElasticsearchClient elasticsearchClient = mock(ElasticsearchClient.class);
		doReturn(true).when(elasticsearchClient).isElasticsearchAvailable();
		doReturn(elasticsearchClient).when(corePlugin).getElasticsearchClient();
		doReturn(false).when(corePlugin).isOnlyLogElasticsearchMetricReports();

		doReturn(true).when(requestMonitorPlugin).isProfilerActive();

		doReturn(1000000d).when(requestMonitorPlugin).getRateLimitServerSpansPerMinute();
		doReturn(mock(ConfigurationOption.class)).when(requestMonitorPlugin).getRateLimitServerSpansPerMinuteOption();
		doReturn(mock(ConfigurationOption.class)).when(requestMonitorPlugin).getRateLimitClientSpansPerMinuteOption();
		when(requestMonitorPlugin.getRateLimitClientSpansPerTypePerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		doReturn(mock(ConfigurationOption.class)).when(requestMonitorPlugin).getProfilerRateLimitPerMinuteOption();
		doReturn(mock(Timer.class)).when(registry).timer(any(MetricName.class));
		doReturn(mock(Meter.class)).when(registry).meter(any(MetricName.class));
		requestMonitor = new RequestMonitor(configuration, registry);
		when(requestMonitorPlugin.isLogSpans()).thenReturn(true);
		when(requestMonitorPlugin.getRequestMonitor()).thenReturn(requestMonitor);

		samplePriorityDeterminingSpanInterceptor = new SamplePriorityDeterminingSpanEventListener(configuration);
		final ReportingSpanEventListener reportingSpanEventListener = new ReportingSpanEventListener(configuration);
		spanCapturingReporter = new SpanCapturingReporter();
		reportingSpanEventListener.addReporter(spanCapturingReporter);
		tracer = RequestMonitorPlugin.createSpanWrappingTracer(getTracer(), configuration, registry,
				TagRecordingSpanEventListener.asList(tags),
				samplePriorityDeterminingSpanInterceptor, reportingSpanEventListener);
		when(requestMonitorPlugin.getTracer()).then((invocation) -> {
			if (corePlugin.isStagemonitorActive()) {
				return tracer;
			} else {
				return NoopTracer.INSTANCE;
			}
		});
		assertTrue(TracingUtils.getTraceContext().isEmpty());
	}

	protected Tracer getTracer() {
		return new MockTracer();
	}

	@After
	public void after() {
		Stagemonitor.getMetric2Registry().removeMatching(Metric2Filter.ALL);
		Stagemonitor.reset();
		assertTrue(TracingUtils.getTraceContext().isEmpty());
	}
}
