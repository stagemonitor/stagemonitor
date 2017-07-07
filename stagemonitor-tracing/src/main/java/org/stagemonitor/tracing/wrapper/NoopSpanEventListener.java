package org.stagemonitor.tracing.wrapper;

public class NoopSpanEventListener extends StatelessSpanEventListener {
	public static NoopSpanEventListener INSTANCE = new NoopSpanEventListener();

	private NoopSpanEventListener() {
	}
}
