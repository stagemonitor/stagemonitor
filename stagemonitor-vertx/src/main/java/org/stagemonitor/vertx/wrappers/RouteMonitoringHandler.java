package org.stagemonitor.vertx.wrappers;

import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.requestmonitor.MonitoredMethodRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

public class RouteMonitoringHandler implements Handler<RoutingContext> {

    private Handler<RoutingContext> delegate;
	private RequestMonitorPlugin requestMonitorPlugin;

    public RouteMonitoringHandler(Handler<RoutingContext> delegate) {
        this.delegate = delegate;
		requestMonitorPlugin = Stagemonitor.getPlugin(RequestMonitorPlugin.class);
    }

    @Override
    public void handle(RoutingContext event) {
        if(!event.normalisedPath().startsWith("/eventbus") && !event.normalisedPath().equals("/")) {
            final RequestMonitor requestMonitor = requestMonitorPlugin.getRequestMonitor();
            startMonitoring(event);
            try {
                delegate.handle(event);
            } catch (Exception e) {
                requestMonitor.recordException(e);
            } finally {
                if(event.failed()){
                    requestMonitor.recordException((Exception) ((io.vertx.ext.web.RoutingContext)event.getDelegate()).failure());
                }
				requestMonitor.monitorStop();
            }
        }
        else{
            delegate.handle(event);
        }
    }

    private void startMonitoring(RoutingContext event){
        final MonitoredMethodRequest monitoredRequest = new MonitoredMethodRequest(Stagemonitor.getConfiguration(), event.normalisedPath(), null);
		requestMonitorPlugin.getRequestMonitor().monitorStart(monitoredRequest);
    }
}
