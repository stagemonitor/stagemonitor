package org.stagemonitor.tracing;

import org.stagemonitor.tracing.reporter.SpanReporter;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SpanCapturingReporter extends SpanReporter {
	private final BlockingQueue<SpanContextInformation> spans = new LinkedBlockingQueue<>();

	@Override
	public void report(SpanContextInformation spanContext, SpanWrapper spanWrapper) throws Exception {
		spans.add(SpanContextInformation.forSpan(spanWrapper));
	}

	@Override
	public boolean isActive(SpanContextInformation spanContext) {
		return true;
	}

	public SpanContextInformation get() throws InterruptedException {
		return spans.poll(500, TimeUnit.MILLISECONDS);
	}
}
