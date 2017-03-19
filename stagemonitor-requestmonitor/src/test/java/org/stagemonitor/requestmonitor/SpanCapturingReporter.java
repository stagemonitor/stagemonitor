package org.stagemonitor.requestmonitor;

import org.stagemonitor.requestmonitor.reporter.SpanReporter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SpanCapturingReporter extends SpanReporter {
	private final BlockingQueue<SpanContextInformation> spans = new LinkedBlockingQueue<>();

	@Override
	public void report(SpanContextInformation spanContext) throws Exception {
		spans.add(spanContext);
	}

	@Override
	public boolean isActive(SpanContextInformation spanContext) {
		return true;
	}

	public SpanContextInformation get() throws InterruptedException {
		return spans.poll(500, TimeUnit.MILLISECONDS);
	}
}
