package org.stagemonitor.vertx;

import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.requestmonitor.MonitoredMethodRequest;

import java.util.Map;

public class MonitoredAsyncMethodRequest extends MonitoredMethodRequest {
    public MonitoredAsyncMethodRequest(Configuration configuration, String methodSignature, MethodExecution methodExecution) {
        super(configuration, methodSignature, methodExecution, null);
    }

    public MonitoredAsyncMethodRequest(Configuration configuration, String methodSignature, MethodExecution methodExecution, Map<String, Object> parameters) {
        super(configuration, methodSignature, methodExecution, parameters);
    }

    @Override
    public boolean isMonitorForwardedExecutions() {
        return true;
    }
}
