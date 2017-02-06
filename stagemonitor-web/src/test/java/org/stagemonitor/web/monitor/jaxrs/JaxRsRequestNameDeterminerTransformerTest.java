package org.stagemonitor.web.monitor.jaxrs;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Filter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.MockTracer;
import org.stagemonitor.requestmonitor.MonitoredMethodRequest;
import org.stagemonitor.requestmonitor.MonitoredRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.SpanCapturingReporter;
import org.stagemonitor.requestmonitor.sampling.SamplePriorityDeterminingSpanInterceptor;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrappingTracer;
import org.stagemonitor.web.WebPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.stagemonitor.requestmonitor.BusinessTransactionNamingStrategy.METHOD_NAME_SPLIT_CAMEL_CASE;

public class JaxRsRequestNameDeterminerTransformerTest {

	private TestResource resource = new TestResource();
	private SpanCapturingReporter requestTraceCapturingReporter = new SpanCapturingReporter();

	@BeforeClass
	@AfterClass
	public static void reset() {
		Stagemonitor.reset();
	}

	private Configuration configuration = mock(Configuration.class);
	private RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
	private WebPlugin webPlugin = mock(WebPlugin.class);
	private CorePlugin corePlugin = mock(CorePlugin.class);
	private RequestMonitor requestMonitor;
	private Metric2Registry registry = new Metric2Registry();


	@Before
	public void before() throws Exception {
		Stagemonitor.startMonitoring(new MeasurementSession("JaxRsRequestNameDeterminerTransformerTest", "testHost", "testInstance"));
		registry.removeMatching(Metric2Filter.ALL);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(configuration.getConfig(WebPlugin.class)).thenReturn(webPlugin);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		when(corePlugin.getApplicationName()).thenReturn("JaxRsRequestNameDeterminerTransformerTest");
		when(corePlugin.getInstanceName()).thenReturn("test");
		when(requestMonitorPlugin.isCollectRequestStats()).thenReturn(true);
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(1000000d);
		when(requestMonitorPlugin.getBusinessTransactionNamingStrategy()).thenReturn(METHOD_NAME_SPLIT_CAMEL_CASE);

		when(webPlugin.getGroupUrls()).thenReturn(Collections.singletonMap(Pattern.compile("(.*).js$"), "*.js"));
		requestMonitor = new RequestMonitor(configuration, registry);
		requestMonitor.addReporter(requestTraceCapturingReporter);
		when(requestMonitorPlugin.getRequestMonitor()).thenReturn(requestMonitor);
		final SpanWrappingTracer tracer = RequestMonitorPlugin.createSpanWrappingTracer(new MockTracer(),
				registry, requestMonitorPlugin, requestMonitor, new ArrayList<>(),
				new SamplePriorityDeterminingSpanInterceptor(configuration, registry));
		when(requestMonitorPlugin.getTracer()).thenReturn(tracer);
	}

	@Test
	public void testSetNameForRestCalls() throws Exception {
		final MonitoredRequest request = new MonitoredMethodRequest(configuration, "override me", () -> resource.getTestString());
		requestMonitor.monitor(request);

		final RequestMonitor.RequestInformation info = requestTraceCapturingReporter.get();

		assertNotNull(info.getSpan());
		assertEquals("Get Test String", info.getOperationName());
	}

	@Path("/")
	public class TestResource {
		@GET
		public String getTestString() {
			return "test";
		}
	}

}
