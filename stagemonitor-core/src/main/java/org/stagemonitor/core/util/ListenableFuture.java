package org.stagemonitor.core.util;

import java.util.concurrent.Future;

public interface ListenableFuture<T> extends Future<T> {
	public void addCallback(FutureCallback<T> callback);

	public static interface FutureCallback<T> {
		public void success(T t);
		public void failure(Throwable t);
	}
}
