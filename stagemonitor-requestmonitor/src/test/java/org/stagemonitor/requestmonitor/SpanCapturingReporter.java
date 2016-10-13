package org.stagemonitor.requestmonitor;

import org.stagemonitor.requestmonitor.reporter.SpanReporter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SpanCapturingReporter extends SpanReporter {
	private final BlockingQueue<RequestTrace> requestTraces = new LinkedBlockingQueue<>();

	public SpanCapturingReporter() {
		RequestMonitor.addRequestTraceReporter(this);
	}

	public SpanCapturingReporter(RequestMonitor requestMonitor) {
		requestMonitor.addReporter(this);
	}

	@Override
	public void report(ReportArguments reportArguments) throws Exception {
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
