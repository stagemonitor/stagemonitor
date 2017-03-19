package org.stagemonitor.requestmonitor;

import org.stagemonitor.requestmonitor.tracing.wrapper.SpanInterceptorFactory;
import org.stagemonitor.requestmonitor.tracing.wrapper.StatelessSpanInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TagRecordingSpanInterceptor extends StatelessSpanInterceptor {
	private final Map<String, Object> tags;

	public static List<SpanInterceptorFactory> asList(Map<String, Object> tags) {
		List<SpanInterceptorFactory> list = new ArrayList<>();
		list.add(new TagRecordingSpanInterceptor(tags));
		return list;
	}

	public TagRecordingSpanInterceptor(Map<String, Object> tags) {
		this.tags = tags;
	}

	@Override
	public String onSetTag(String key, String value) {
		tags.put(key, value);
		return value;
	}

	@Override
	public boolean onSetTag(String key, boolean value) {
		tags.put(key, value);
		return value;
	}

	@Override
	public Number onSetTag(String key, Number value) {
		tags.put(key, value);
		return value;
	}
}
