package org.stagemonitor.vertx;

import com.uber.jaeger.context.TraceContext;
import rx.Subscriber;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RequestKeeper {
    private static RequestKeeper ourInstance = new RequestKeeper();

    public static RequestKeeper getInstance() {
        return ourInstance;
    }

    private ConcurrentHashMap<Object, SavedTraceContext> savedContexts;
    private ConcurrentHashMap<SavedTraceContext, List<Subscriber<?>>> subscriberLists;

    private RequestKeeper() {
        savedContexts = new ConcurrentHashMap<>();
        subscriberLists = new ConcurrentHashMap<>();
    }

    public boolean containsKey(Object id){
        return savedContexts.containsKey(id);
    }

    public SavedTraceContext getSavedContext(Object id){
        return savedContexts.get(id);
    }

    public void storeContext(Object id, TraceContext context){
        savedContexts.put(id, new SavedTraceContext(context));
    }

    public void addSubscriber(SavedTraceContext key, Subscriber<?> subscriber){
        if(!subscriberLists.containsKey(key)){
            subscriberLists.put(key, new LinkedList<>());
        }
        subscriberLists.get(key).add(subscriber);
    }

    public void removeSubscriber(SavedTraceContext key, Subscriber<?> subscriber){
        if(subscriberLists.containsKey(key)){
            subscriberLists.get(key).remove(subscriber);
        }
    }

    public boolean isSubscriberListEmpty(SavedTraceContext key){
        return !subscriberLists.containsKey(key) || subscriberLists.get(key).isEmpty();
    }
}
