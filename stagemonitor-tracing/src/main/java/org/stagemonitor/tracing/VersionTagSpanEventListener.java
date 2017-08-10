package org.stagemonitor.tracing;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.tracing.wrapper.StatelessSpanEventListener;

public class VersionTagSpanEventListener extends StatelessSpanEventListener {

	private final String version = Stagemonitor.getPlugin(TracingPlugin.class).getVersion();

	@Override
	public void onStart(SpanWrapper spanWrapper) {
		spanWrapper.setTag("stagemonitor.version", version);
	}
}
