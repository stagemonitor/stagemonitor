package org.stagemonitor.vertx.wrappers.rxJava;

import io.vertx.rxjava.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.requestmonitor.MonitoredMethodRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.vertx.RequestKeeper;
import org.stagemonitor.vertx.VertxPlugin;
import org.stagemonitor.vertx.utils.SavedTraceContext;
import rx.Subscriber;

import java.util.LinkedHashMap;
import java.util.Map;

public class MessageConsumerMonitoringSubscriber extends Subscriber<Message<?>> {

    private final Subscriber<Message<?>> actual;

    private Logger logger = LoggerFactory.getLogger(MessageConsumerMonitoringSubscriber.class);
    private VertxPlugin vertxPlugin;
    private RequestMonitorPlugin requestMonitorPlugin;

    public MessageConsumerMonitoringSubscriber(Subscriber<Message<?>> actual) {
        super(actual);
        this.actual = actual;
		vertxPlugin = Stagemonitor.getPlugin(VertxPlugin.class);
		requestMonitorPlugin = Stagemonitor.getPlugin(RequestMonitorPlugin.class);
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
    public void onNext(Message<?> message) {
		final RequestMonitor requestMonitor = requestMonitorPlugin.getRequestMonitor();
        SavedTraceContext context = RequestKeeper.getInstance().getSavedContext(message.body());
        if(context != null && context.getCurrentSpan() != null){
            context.getTraceContext().push(context.getCurrentSpan());
        }
		try {
			startMonitoring(message);
		} catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		try{
            actual.onNext(message);
        }
        catch (Throwable e){
            requestMonitor.recordException((Exception) e);
        }
        finally {
			requestMonitor.monitorStop();
			if(context != null && context.getCurrentSpan() != null){
				context.getTraceContext().pop();
			}
        }
    }

    private void startMonitoring(Message<?> message) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        final String requestName = vertxPlugin.getRequestNamer().getRequestName((io.vertx.core.eventbus.Message<?>) message.getDelegate());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("message", message.body());
        final MonitoredMethodRequest monitoredRequest = new MonitoredMethodRequest(Stagemonitor.getConfiguration(), requestName, null, params);
        final RequestMonitorPlugin requestMonitorPlugin = Stagemonitor.getPlugin(RequestMonitorPlugin.class);
        requestMonitorPlugin.getRequestMonitor().monitorStart(monitoredRequest);
    }
}
