package org.stagemonitor.tracing.reporter;

import org.stagemonitor.tracing.B3HeaderFormat;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.wrapper.SpanEventListener;
import org.stagemonitor.tracing.wrapper.SpanEventListenerFactory;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.util.StringUtils;

import java.util.concurrent.TimeUnit;

import io.opentracing.tag.Tags;

public class ReadbackSpanEventListener implements SpanEventListener {

	private static final double MILLISECOND_IN_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

	private final ReportingSpanEventListener reportingSpanEventListener;
	private final TracingPlugin tracingPlugin;
	private ReadbackSpan readbackSpan = new ReadbackSpan();

	public ReadbackSpanEventListener(ReportingSpanEventListener reportingSpanEventListener, TracingPlugin tracingPlugin) {
		this.reportingSpanEventListener = reportingSpanEventListener;
		this.tracingPlugin = tracingPlugin;
	}

	public static class Factory implements SpanEventListenerFactory {

		private final ReportingSpanEventListener reportingSpanEventListener;
		private final TracingPlugin tracingPlugin;

		public Factory(ReportingSpanEventListener reportingSpanEventListener, TracingPlugin tracingPlugin) {
			this.reportingSpanEventListener = reportingSpanEventListener;
			this.tracingPlugin = tracingPlugin;
		}

		@Override
		public ReadbackSpanEventListener create() {
			return new ReadbackSpanEventListener(reportingSpanEventListener, tracingPlugin);
		}
	}

	@Override
	public void onStart(SpanWrapper spanWrapper) {
		if (!isAnyReporterActive(spanWrapper)) {
			readbackSpan = null;
		}
		if (readbackSpan != null) {
			SpanContextInformation.forSpan(spanWrapper).setReadbackSpan(readbackSpan);
			tracingPlugin.getTracer().inject(spanWrapper.context(), B3HeaderFormat.INSTANCE, new B3HeaderFormat.B3InjectAdapter() {
				@Override
				public void setParentId(String value) {
					readbackSpan.setParentId(value);
				}

				@Override
				public void setSpanId(String value) {
					readbackSpan.setId(value);
				}

				@Override
				public void setTraceId(String value) {
					readbackSpan.setTraceId(value);
				}
			});
		}
	}

	@Override
	public String onSetTag(String key, String value) {
		if (readbackSpan != null) {
			readbackSpan.setTag(key, value);
		}
		return value;
	}

	@Override
	public boolean onSetTag(String key, boolean value) {
		if (readbackSpan != null) {
			readbackSpan.setTag(key, value);
		}
		return value;
	}

	@Override
	public Number onSetTag(String key, Number value) {
		if (isNotSampled(key, value)) {
			readbackSpan = null;
		} else if (readbackSpan != null) {
			readbackSpan.setTag(key, value);
		}
		return value;
	}

	@Override
	public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
		if (readbackSpan != null) {
			readbackSpan.setName(operationName);
			readbackSpan.setDuration(durationNanos / MILLISECOND_IN_NANOS);
			final String timestamp = StringUtils.timestampAsIsoString(spanWrapper.getStartTimestampMillis());
			readbackSpan.setTimestamp(timestamp);
		}
	}

	private boolean isNotSampled(String key, Number value) {
		return Tags.SAMPLING_PRIORITY.getKey().equals(key) && value.shortValue() <= 0;
	}

	private boolean isAnyReporterActive(SpanWrapper spanWrapper) {
		return reportingSpanEventListener.isAnyReporterActive(SpanContextInformation.forSpan(spanWrapper));
	}

	public ReadbackSpan getReadbackSpan() {
		return readbackSpan;
	}
}
