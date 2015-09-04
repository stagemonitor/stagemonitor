package org.stagemonitor.core.util;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CompletedFuture<T> implements Future<T> {
	private final T result;

	public CompletedFuture(final T result) {
		this.result = result;
	}

	@Override
	public boolean cancel(final boolean b) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public T get() {
		return this.result;
	}

	@Override
	public T get(final long l, final TimeUnit timeUnit) {
		return get();
	}
}
