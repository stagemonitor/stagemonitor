package org.stagemonitor.objectpool;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectPool implements Closeable {

	private final Map<Class<? extends Recyclable>, RecyclableDescription<?>> registeredObjectFactories = new ConcurrentHashMap<Class<? extends Recyclable>, RecyclableDescription<?>>();

	private final ThreadLocal<Map<Class<? extends Recyclable>, FixedSizeStack<Recyclable>>> objectPools = new ThreadLocal<Map<Class<? extends Recyclable>, FixedSizeStack<Recyclable>>>() {
		@Override
		protected Map<Class<? extends Recyclable>, FixedSizeStack<Recyclable>> initialValue() {
			return new HashMap<Class<? extends Recyclable>, FixedSizeStack<Recyclable>>();
		}
	};

	public <T extends Recyclable> void registerRecyclableObjectFactory(RecyclableObjectFactory<T> recyclableObjectFactory, Class<T> type, int maxNumPooledObjectsPerThread) {
		registeredObjectFactories.put(type, new RecyclableDescription<T>(recyclableObjectFactory, maxNumPooledObjectsPerThread));
	}

	private static class RecyclableDescription<T extends Recyclable> {
		final RecyclableObjectFactory<T> recyclableObjectFactory;
		final int maxNumPooledObjectsPerThread;

		private RecyclableDescription(RecyclableObjectFactory<T> recyclableObjectFactory, int maxNumPooledObjectsPerThread) {
			this.recyclableObjectFactory = recyclableObjectFactory;
			this.maxNumPooledObjectsPerThread = maxNumPooledObjectsPerThread;
		}
	}

	public <T extends Recyclable> T getInstance(Class<T> type) {
		T obj = this.<T>getObjectPool(type).pop();
		if (obj != null) {
			return obj;
		} else {
			return (T) registeredObjectFactories.get(type).recyclableObjectFactory.createInstance();
		}
	}

	public <T extends Recyclable> void recycle(T obj, Class<? extends Recyclable> type) {
		obj.resetState();
		getObjectPool(type).push(obj);
	}

	private <T extends Recyclable> FixedSizeStack<T> getObjectPool(Class<? extends Recyclable> type) {
		FixedSizeStack<Recyclable> objectPool = this.objectPools.get().get(type);
		if (objectPool == null) {
			objectPool = new FixedSizeStack<Recyclable>(getMaxSize(type));
			this.objectPools.get().put(type, objectPool);
		}
		return (FixedSizeStack<T>) objectPool;
	}

	private int getMaxSize(Class<? extends Recyclable> type) {
		return registeredObjectFactories.get(type).maxNumPooledObjectsPerThread;
	}

	int getCurrentThreadsQueueSize(Class<? extends Recyclable> type) {
		return getObjectPool(type).size();
	}

	@Override
	public void close() {
		objectPools.remove();
	}

	// inspired by https://stackoverflow.com/questions/7727919/creating-a-fixed-size-stack/7728703#7728703
	public static class FixedSizeStack<T> {
		private final T[] stack;
		private int top;

		FixedSizeStack(int maxSize) {
			this.stack = (T[]) new Object[maxSize];
			this.top = -1;
		}

		boolean push(T obj) {
			int newTop = top + 1;
			if (newTop >= stack.length) {
				return false;
			}
			stack[newTop] = obj;
			top = newTop;
			return true;
		}

		T pop() {
			if (top < 0) return null;
			T obj = stack[top--];
			stack[top + 1] = null;
			return obj;
		}

		int size() {
			return top + 1;
		}
	}
}
