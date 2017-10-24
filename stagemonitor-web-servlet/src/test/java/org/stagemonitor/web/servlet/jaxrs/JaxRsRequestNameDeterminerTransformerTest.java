package org.stagemonitor.web.servlet.jaxrs;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.Metric2Filter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.tracing.GlobalTracerTestHelper;
import org.stagemonitor.tracing.MonitoredMethodRequest;
import org.stagemonitor.tracing.MonitoredRequest;
import org.stagemonitor.tracing.RequestMonitor;
import org.stagemonitor.tracing.SpanCapturingReporter;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.reporter.ReportingSpanEventListener;
import org.stagemonitor.tracing.sampling.SamplePriorityDeterminingSpanEventListener;
import org.stagemonitor.tracing.wrapper.SpanWrappingTracer;
import org.stagemonitor.web.servlet.ServletPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.stagemonitor.tracing.BusinessTransactionNamingStrategy.METHOD_NAME_SPLIT_CAMEL_CASE;

public class JaxRsRequestNameDeterminerTransformerTest {

	private TestResource resource = new TestResource();
	private SpanCapturingReporter spanCapturingReporter;

	@BeforeClass
	@AfterClass
	public static void reset() {
		Stagemonitor.reset();
	}

	private ConfigurationRegistry configuration = mock(ConfigurationRegistry.class);
	private TracingPlugin tracingPlugin = mock(TracingPlugin.class);
	private ServletPlugin servletPlugin = mock(ServletPlugin.class);
	private CorePlugin corePlugin = mock(CorePlugin.class);
	private RequestMonitor requestMonitor;
	private Metric2Registry registry = new Metric2Registry();


	@Before
	public void before() throws Exception {
		Stagemonitor.reset(new MeasurementSession("JaxRsRequestNameDeterminerTransformerTest", "testHost", "testInstance"));
		registry.removeMatching(Metric2Filter.ALL);
		when(configuration.getConfig(TracingPlugin.class)).thenReturn(tracingPlugin);
		when(configuration.getConfig(ServletPlugin.class)).thenReturn(servletPlugin);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		when(corePlugin.getApplicationName()).thenReturn("JaxRsRequestNameDeterminerTransformerTest");
		when(corePlugin.getInstanceName()).thenReturn("test");
		when(tracingPlugin.getDefaultRateLimitSpansPerMinute()).thenReturn(1000000d);
		when(tracingPlugin.getDefaultRateLimitSpansPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(tracingPlugin.getDefaultRateLimitSpansPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(tracingPlugin.getDefaultRateLimitSpansPercentOption()).thenReturn(mock(ConfigurationOption.class));
		when(tracingPlugin.getRateLimitSpansPerMinutePercentPerTypeOption()).thenReturn(mock(ConfigurationOption.class));
		when(tracingPlugin.getDefaultRateLimitSpansPercent()).thenReturn(1.0);
		when(tracingPlugin.getRateLimitSpansPerMinutePercentPerType()).thenReturn(Collections.emptyMap());
		when(tracingPlugin.getProfilerRateLimitPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(tracingPlugin.getBusinessTransactionNamingStrategy()).thenReturn(METHOD_NAME_SPLIT_CAMEL_CASE);

		when(servletPlugin.getGroupUrls()).thenReturn(Collections.singletonMap(Pattern.compile("(.*).js$"), "*.js"));
		requestMonitor = new RequestMonitor(configuration, registry);
		when(tracingPlugin.getRequestMonitor()).thenReturn(requestMonitor);
		final ReportingSpanEventListener reportingSpanEventListener = new ReportingSpanEventListener(configuration);
		spanCapturingReporter = new SpanCapturingReporter();
		reportingSpanEventListener.addReporter(spanCapturingReporter);
		final SpanWrappingTracer tracer = TracingPlugin.createSpanWrappingTracer(new MockTracer(),
				configuration, registry, new ArrayList<>(),
				new SamplePriorityDeterminingSpanEventListener(configuration), reportingSpanEventListener);
		GlobalTracerTestHelper.resetGlobalTracer();
		GlobalTracer.register(tracer);
		when(tracingPlugin.getTracer()).thenReturn(tracer);
	}

	@After
	public void after() {
		GlobalTracerTestHelper.resetGlobalTracer();
	}

	@Test
	public void testSetNameForRestCalls() throws Exception {
		final MonitoredRequest request = new MonitoredMethodRequest(configuration, "override me", () -> resource.getTestString());
		requestMonitor.monitor(request);

		final SpanContextInformation info = spanCapturingReporter.get();

		assertNotNull(info);
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
