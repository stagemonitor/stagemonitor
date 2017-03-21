package org.stagemonitor.vertx.wrappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.vertx.RequestKeeper;
import rx.Subscriber;

public class ResponseMonitoringSubscriber<T> extends Subscriber<T> {

    private final Subscriber<T> actual;

    private RequestMonitor.RequestInformation<? extends RequestTrace> info;
	private RequestMonitorPlugin requestMonitorPlugin;
    private Logger logger = LoggerFactory.getLogger(ResponseMonitoringSubscriber.class);

    public ResponseMonitoringSubscriber(Subscriber<T> actual) {
        super(actual);
        this.actual = actual;
		requestMonitorPlugin = Stagemonitor.getPlugin(RequestMonitorPlugin.class);
        info = RequestMonitor.get().getRequestInfo();
        if(info != null){
            RequestKeeper.getInstance().addSubscriber(info, this);
        }
    }

    @Override
    public void onCompleted() {
        actual.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
		requestMonitorPlugin.getRequestMonitor().recordException((Exception) e);
        actual.onError(e);
    }

    @Override
    public void onNext(T t) {
        actual.onNext(t);
        if(info != null) {
            RequestKeeper.getInstance().removeSubscriber(info, this);
            if (RequestKeeper.getInstance().isSubscriberListEmpty(info)) {
				requestMonitorPlugin.getRequestMonitor().setRequestInfo(info);
				requestMonitorPlugin.getRequestMonitor().monitorStop();
            }
        }
    }
}
