package org.stagemonitor.requestmonitor;

import org.stagemonitor.requestmonitor.reporter.RequestTraceReporter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RequestTraceCapturingReporter extends RequestTraceReporter {
	private final BlockingQueue<RequestTrace> requestTraces = new LinkedBlockingQueue<>();

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
//		return requestTraces.take();
		return requestTraces.poll(500, TimeUnit.MILLISECONDS);
	}
}
