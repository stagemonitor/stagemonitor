package org.stagemonitor.tracing.tagexclude;

import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.wrapper.StatelessSpanEventListener;

public class TagExcludingSpanEventListener extends StatelessSpanEventListener {

	private final TracingPlugin tracingPlugin;

	public TagExcludingSpanEventListener(TracingPlugin tracingPlugin) {
		this.tracingPlugin = tracingPlugin;
	}

	@Override
	public Number onSetTag(String key, Number value) {
		if (tracingPlugin.getExcludedTags().contains(key)) {
			return null;
		} else {
			return value;
		}
	}

	@Override
	public String onSetTag(String key, String value) {
		if (tracingPlugin.getExcludedTags().contains(key)) {
			return null;
		} else {
			return value;
		}
	}
}
