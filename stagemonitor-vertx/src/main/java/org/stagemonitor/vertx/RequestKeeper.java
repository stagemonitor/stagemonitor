package org.stagemonitor.vertx;

import com.uber.jaeger.context.TraceContext;

import org.stagemonitor.vertx.utils.SavedTraceContext;

import java.util.concurrent.ConcurrentHashMap;

public class RequestKeeper {
	private static RequestKeeper ourInstance = new RequestKeeper();
	private ConcurrentHashMap<Object, SavedTraceContext> savedContexts;

	private RequestKeeper() {
		savedContexts = new ConcurrentHashMap<>();
	}

	public static RequestKeeper getInstance() {
		return ourInstance;
	}

	public SavedTraceContext getSavedContext(Object id) {
		SavedTraceContext context = savedContexts.get(id);
		savedContexts.remove(id);
		return context;
	}

	public void storeContext(Object id, TraceContext context) {
		savedContexts.putIfAbsent(id, new SavedTraceContext(context));
	}

	public boolean containsKey(Object message) {
		return savedContexts.containsKey(message);
	}
}
