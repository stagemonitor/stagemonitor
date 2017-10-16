package org.stagemonitor.tracing;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class RequestMonitorTest extends AbstractRequestMonitorTest {

	@Test
	public void testDeactivated() throws Exception {
		doReturn(false).when(corePlugin).isStagemonitorActive();

		final SpanContextInformation spanContext = requestMonitor.monitor(createMonitoredRequest());

		assertNull(spanContext);
	}

	@Test
	public void testRecordException() throws Exception {
		final MonitoredRequest monitoredRequest = createMonitoredRequest();
		doThrow(new RuntimeException("test")).when(monitoredRequest).execute();
		try {
			requestMonitor.monitor(monitoredRequest);
		} catch (Exception e) {
		}
		assertEquals("java.lang.RuntimeException", tags.get("exception.class"));
		assertEquals("test", tags.get("exception.message"));
		assertNotNull(tags.get("exception.stack_trace"));
	}

	@Test
	public void testInternalMetricsDeactive() throws Exception {
		internalMonitoringTestHelper(false);
	}

	@Test
	public void testInternalMetricsActive() throws Exception {
		doReturn(true).when(corePlugin).isInternalMonitoringActive();

		requestMonitor.monitor(createMonitoredRequest());
		verify(registry).timer(name("internal_overhead_request_monitor").build());
	}

	private void internalMonitoringTestHelper(boolean active) throws Exception {
		doReturn(active).when(corePlugin).isInternalMonitoringActive();
		requestMonitor.monitor(createMonitoredRequest());
		verify(registry, times(active ? 1 : 0)).timer(name("internal_overhead_request_monitor").build());
	}

	private MonitoredRequest createMonitoredRequest() throws Exception {
		return Mockito.spy(new MonitoredMethodRequest(configuration, "test", () -> {
		}));
	}
}
