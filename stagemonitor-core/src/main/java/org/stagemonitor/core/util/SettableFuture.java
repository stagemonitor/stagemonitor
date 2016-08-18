package org.stagemonitor.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SettableFuture<T> implements ListenableFuture<T> {
	private List<FutureCallback<T>> callbacks = null;
	private volatile boolean cancelled = false;
	private volatile boolean done = false;

	private T result;
	private Throwable exception;

	@Override
	public synchronized void addCallback(FutureCallback<T> callback) {
		if (!done) {
			if (this.callbacks == null) {
				this.callbacks = new ArrayList<FutureCallback<T>>();
			}
			this.callbacks.add(callback);
		} else {
			executeCallback(callback);
		}
	}

	@Override
	public synchronized boolean cancel(boolean mayInterruptIfRunning) {
		if (!done && !cancelled) {
			cancelled = true;
            exception = new CancellationException();
			notify();
            executeCallbacks();
			return true;
		}
		return false;
	}

	@Override
	public synchronized boolean isCancelled() {
		return cancelled;
	}

	@Override
	public synchronized boolean isDone() {
		return done;
	}

	private T getResult() throws ExecutionException {
		if (this.exception != null) {
			throw new ExecutionException(this.exception);
		} else {
			return result;
		}
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		if (!done && !cancelled) {
			synchronized (this) {
				while (!done && !cancelled) {
					wait();
				}
			}
		}

		if (done) {
			return getResult();
		} else if (cancelled) {
			throw new CancellationException();
		} else {
			throw new ExecutionException(new RuntimeException("Unknown SettableFuture error"));
		}
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (done) {
			return getResult();
		}

		synchronized (this) {
			wait(unit.toMillis(timeout));
			if (done) {
				return get();
			} else {
				throw new TimeoutException();
			}
		}
	}

	public synchronized boolean set(T result) {
		if (!done && !cancelled) {
			this.result = result;
			this.done = true;
			notify();
			executeCallbacks();
            return true;
		}
        return false;
	}

	public synchronized boolean setException(Throwable exception) {
		if (!done && !cancelled) {
			this.exception = exception;
			this.done = true;
			notify();
			executeCallbacks();
            return true;
		}
        return false;
	}

	private void executeCallbacks() {
		if (callbacks == null) {
			return;
		}
		for (FutureCallback<T> callback : callbacks) {
			executeCallback(callback);
		}
		callbacks = null;
	}

	private void executeCallback(FutureCallback<T> callback) {
		if (this.exception != null) {
			callback.failure(this.exception);
		} else {
			callback.success(this.result);
		}
	}
}
