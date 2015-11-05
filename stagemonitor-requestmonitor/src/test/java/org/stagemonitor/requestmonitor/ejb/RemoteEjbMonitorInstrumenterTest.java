package org.stagemonitor.requestmonitor.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.ejb.Remote;

import com.codahale.metrics.SharedMetricRegistries;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.requestmonitor.RequestTraceReporter;

public class RemoteEjbMonitorInstrumenterTest {

	private RemoteInterface remote = new RemoteInterfaceImpl();

	@BeforeClass
	@AfterClass
	public static void reset() {
		Stagemonitor.reset();
		SharedMetricRegistries.clear();
	}

	@Test
	public void testMonitorRemoteCalls() throws Exception {
		final RequestTraceCapturingReporter requestTraceCapturingReporter = new RequestTraceCapturingReporter();
		RequestMonitor.addRequestTraceReporter(requestTraceCapturingReporter);

		remote.foo();

		RequestTrace requestTrace = requestTraceCapturingReporter.get();
		assertNotNull(requestTrace);
		assertEquals("RemoteEjbMonitorInstrumenterTest$RemoteInterfaceImpl#foo", requestTrace.getName());
		assertEquals("void org.stagemonitor.requestmonitor.ejb.RemoteEjbMonitorInstrumenterTest$RemoteInterfaceImpl.foo()",
				requestTrace.getCallStack().getChildren().get(0).getSignature());
	}

	@Test
	public void testDontMonitorToString() throws Exception {
		final RequestTraceCapturingReporter requestTraceCapturingReporter = new RequestTraceCapturingReporter();
		RequestMonitor.addRequestTraceReporter(requestTraceCapturingReporter);

		remote.toString();

		RequestTrace requestTrace = requestTraceCapturingReporter.get();
		assertNull(requestTrace);
	}

	private interface RemoteInterface {
		void foo();
	}

	@Remote(RemoteInterface.class)
	public class RemoteInterfaceImpl implements RemoteInterface {

		@Override
		public void foo() {
		}

		@Override
		public String toString() {
			return super.toString();
		}
	}

	private static class RequestTraceCapturingReporter implements RequestTraceReporter {
		private final BlockingQueue<RequestTrace> requestTraces = new ArrayBlockingQueue<RequestTrace>(1);

		@Override
		public <T extends RequestTrace> void reportRequestTrace(T requestTrace) throws Exception {
			requestTraces.add(requestTrace);
		}

		@Override
		public <T extends RequestTrace> boolean isActive(T requestTrace) {
			return true;
		}

		public RequestTrace get() throws InterruptedException {
			return requestTraces.poll(500, TimeUnit.MILLISECONDS);
		}
	}
}
