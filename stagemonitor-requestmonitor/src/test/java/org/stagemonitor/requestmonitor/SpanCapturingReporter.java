package org.stagemonitor.requestmonitor;

import org.stagemonitor.requestmonitor.reporter.SpanReporter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SpanCapturingReporter extends SpanReporter {
	private final BlockingQueue<RequestMonitor.RequestInformation> requestTraces = new LinkedBlockingQueue<>();

	public SpanCapturingReporter() {
		RequestMonitor.addRequestTraceReporter(this);
	}

	public SpanCapturingReporter(RequestMonitor requestMonitor) {
		requestMonitor.addReporter(this);
	}

	@Override
	public void report(RequestMonitor.RequestInformation requestInformation) throws Exception {
		requestTraces.add(requestInformation);
	}

	@Override
	public boolean isActive(RequestMonitor.RequestInformation requestInformation) {
		return true;
	}

	public RequestMonitor.RequestInformation get() throws InterruptedException {
		return requestTraces.poll(500, TimeUnit.MILLISECONDS);
	}
}
