package org.stagemonitor.tracing;

import org.stagemonitor.tracing.wrapper.SpanEventListenerFactory;
import org.stagemonitor.tracing.wrapper.StatelessSpanEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TagRecordingSpanEventListener extends StatelessSpanEventListener {
	private final Map<String, Object> tags;

	public static List<SpanEventListenerFactory> asList(Map<String, Object> tags) {
		List<SpanEventListenerFactory> list = new ArrayList<>();
		list.add(new TagRecordingSpanEventListener(tags));
		return list;
	}

	public TagRecordingSpanEventListener(Map<String, Object> tags) {
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
