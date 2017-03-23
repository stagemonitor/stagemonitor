package org.stagemonitor.vertx;

import com.uber.jaeger.context.TraceContext;
import org.stagemonitor.vertx.utils.SavedTraceContext;

import java.util.concurrent.ConcurrentHashMap;

public class RequestKeeper {
    private static RequestKeeper ourInstance = new RequestKeeper();

    public static RequestKeeper getInstance() {
        return ourInstance;
    }

    private ConcurrentHashMap<Object, SavedTraceContext> savedContexts;

    private RequestKeeper() {
        savedContexts = new ConcurrentHashMap<>();
    }

    public SavedTraceContext getSavedContext(Object id){
        return savedContexts.get(id);
    }

    public void storeContext(Object id, TraceContext context){
        savedContexts.put(id, new SavedTraceContext(context));
    }

	public boolean containsKey(Object message) {
		return savedContexts.containsKey(message);
	}
}
