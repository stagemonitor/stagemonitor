package org.stagemonitor.requestmonitor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.stagemonitor.requestmonitor.reporter.RequestTraceReporter;

public class RequestTraceCapturingReporter implements RequestTraceReporter {
	private final BlockingQueue<RequestTrace> requestTraces = new ArrayBlockingQueue<RequestTrace>(1);

	public RequestTraceCapturingReporter() {
		RequestMonitor.addRequestTraceReporter(this);
	}

	public RequestTraceCapturingReporter(RequestMonitor requestMonitor) {
		requestMonitor.addReporter(this);
	}

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
