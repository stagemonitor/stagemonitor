package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.tracing.B3HeaderFormat;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanEventListener;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanEventListenerFactory;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;

import io.opentracing.tag.Tags;

public class ReadbackSpanEventListener implements SpanEventListener {

	private final ReportingSpanEventListener reportingSpanEventListener;
	private final RequestMonitorPlugin requestMonitorPlugin;
	private ReadbackSpan readbackSpan = new ReadbackSpan();

	public ReadbackSpanEventListener(ReportingSpanEventListener reportingSpanEventListener, RequestMonitorPlugin requestMonitorPlugin) {
		this.reportingSpanEventListener = reportingSpanEventListener;
		this.requestMonitorPlugin = requestMonitorPlugin;
	}

	public static class Factory implements SpanEventListenerFactory {

		private final ReportingSpanEventListener reportingSpanEventListener;
		private final RequestMonitorPlugin requestMonitorPlugin;

		public Factory(ReportingSpanEventListener reportingSpanEventListener, RequestMonitorPlugin requestMonitorPlugin) {
			this.reportingSpanEventListener = reportingSpanEventListener;
			this.requestMonitorPlugin = requestMonitorPlugin;
		}

		@Override
		public SpanEventListener create() {
			return new ReadbackSpanEventListener(reportingSpanEventListener, requestMonitorPlugin);
		}
	}

	@Override
	public void onStart(SpanWrapper spanWrapper) {
		if (!isAnyReporterActive(spanWrapper)) {
			readbackSpan = null;
		}
		if (readbackSpan != null) {
			requestMonitorPlugin.getTracer().inject(spanWrapper.context(), B3HeaderFormat.INSTANCE, new B3HeaderFormat.B3InjectAdapter() {
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
			readbackSpan.setDuration(durationNanos);
			final String timestamp = StringUtils.timestampAsIsoString(spanWrapper.getStartTimestampMillis());
			readbackSpan.setTimestamp(timestamp);
			SpanContextInformation.forSpan(spanWrapper).setReadbackSpan(readbackSpan);
		}
	}

	private boolean isNotSampled(String key, Number value) {
		return Tags.SAMPLING_PRIORITY.getKey().equals(key) && value.shortValue() <= 0;
	}

	private boolean isAnyReporterActive(SpanWrapper spanWrapper) {
		return reportingSpanEventListener.isAnyReporterActive(SpanContextInformation.forSpan(spanWrapper));
	}
}
