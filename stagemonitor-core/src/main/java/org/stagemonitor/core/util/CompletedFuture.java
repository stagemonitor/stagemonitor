package org.stagemonitor.core.util;

public class CompletedFuture<T> extends SettableFuture<T> {
	public CompletedFuture() {
		set(null);
	}

	public CompletedFuture(final T result) {
		set(result);
	}

	public CompletedFuture(final Throwable exception) {
		setException(exception);
	}
}
