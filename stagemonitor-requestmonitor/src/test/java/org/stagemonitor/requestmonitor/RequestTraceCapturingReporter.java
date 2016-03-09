package org.stagemonitor.requestmonitor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.stagemonitor.requestmonitor.reporter.RequestTraceReporter;

public class RequestTraceCapturingReporter extends RequestTraceReporter {
	private final BlockingQueue<RequestTrace> requestTraces = new ArrayBlockingQueue<RequestTrace>(1);

	public RequestTraceCapturingReporter() {
		RequestMonitor.addRequestTraceReporter(this);
	}

	public RequestTraceCapturingReporter(RequestMonitor requestMonitor) {
		requestMonitor.addReporter(this);
	}

	@Override
	public void reportRequestTrace(ReportArguments reportArguments) throws Exception {
		requestTraces.add(reportArguments.getRequestTrace());
	}

	@Override
	public boolean isActive(IsActiveArguments isActiveArguments) {
		return true;
	}

	public RequestTrace get() throws InterruptedException {
		return requestTraces.poll(500, TimeUnit.MILLISECONDS);
	}
}
