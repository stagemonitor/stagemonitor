package org.stagemonitor.vertx.wrappers;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
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

public class MessageConsumerMonitoringHandler implements Handler<Message<?>> {

    private final Handler<Message<?>> delegate;

    private Logger logger = LoggerFactory.getLogger(MessageConsumerMonitoringHandler.class);
    private VertxPlugin vertxPlugin;
    private RequestMonitorPlugin requestMonitorPlugin;

    public MessageConsumerMonitoringHandler(Handler<Message<?>> delegate) {
        this.delegate = delegate;
		vertxPlugin = Stagemonitor.getPlugin(VertxPlugin.class);
		requestMonitorPlugin = Stagemonitor.getPlugin(RequestMonitorPlugin.class);
    }

	@Override
	public void handle(Message<?> message) {
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
			delegate.handle(message);
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
		final String requestName = vertxPlugin.getRequestNamer().getRequestName(message);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("message", message.body());
		final MonitoredMethodRequest monitoredRequest = new MonitoredMethodRequest(Stagemonitor.getConfiguration(), requestName, null, params);
		final RequestMonitorPlugin requestMonitorPlugin = Stagemonitor.getPlugin(RequestMonitorPlugin.class);
		requestMonitorPlugin.getRequestMonitor().monitorStart(monitoredRequest);
	}
}
