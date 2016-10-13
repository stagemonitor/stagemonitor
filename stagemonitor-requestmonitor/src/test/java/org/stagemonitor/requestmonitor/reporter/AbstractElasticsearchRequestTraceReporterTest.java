package org.stagemonitor.requestmonitor.reporter;

import org.junit.Before;
import org.slf4j.Logger;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.stagemonitor.requestmonitor.reporter.ServerRequestMetricsReporter.getTimerMetricName;

public class AbstractElasticsearchRequestTraceReporterTest {
	protected ElasticsearchClient elasticsearchClient;
	protected RequestMonitorPlugin requestMonitorPlugin;
	protected Logger requestTraceLogger;
	protected Metric2Registry registry;
	protected Configuration configuration;
	protected CorePlugin corePlugin;

	@Before
	public void setUp() throws Exception {
		configuration = mock(Configuration.class);
		corePlugin = mock(CorePlugin.class);
		requestMonitorPlugin = mock(RequestMonitorPlugin.class);

		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(1000000d);
		when(requestMonitorPlugin.getOnlyReportRequestsWithNameToElasticsearch()).thenReturn(Collections.singleton("Report Me"));
		when(corePlugin.getElasticsearchUrl()).thenReturn("http://localhost:9200");
		when(corePlugin.getElasticsearchUrls()).thenReturn(Collections.singletonList("http://localhost:9200"));
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient = mock(ElasticsearchClient.class));
		registry = new Metric2Registry();
		when(corePlugin.getMetricRegistry()).thenReturn(registry);
		requestTraceLogger = mock(Logger.class);
	}

	protected RequestTrace createTestRequestTraceWithCallTree(long executionTime) {
		final RequestTrace requestTrace = new RequestTrace(UUID.randomUUID().toString(), new MeasurementSession("ERTRT", "test", "test"), requestMonitorPlugin);
		requestTrace.setCallStack(CallStackElement.createRoot("test"));
		requestTrace.setName("Report Me");
		requestTrace.setExecutionTime(executionTime);
		registry.timer(getTimerMetricName(requestTrace.getName())).update(executionTime, TimeUnit.NANOSECONDS);
		return requestTrace;
	}
}
