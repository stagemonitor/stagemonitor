package org.stagemonitor.requestmonitor;

import org.stagemonitor.requestmonitor.tracing.wrapper.SpanInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class TagRecordingSpanInterceptor extends SpanInterceptor implements Callable<SpanInterceptor> {
	private final Map<String, Object> tags;

	public static List<Callable<SpanInterceptor>> asList(Map<String, Object> tags) {
		List<Callable<SpanInterceptor>> list = new ArrayList<>();
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

	@Override
	public SpanInterceptor call() throws Exception {
		return this;
	}
}
