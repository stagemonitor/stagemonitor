package org.stagemonitor.collector.core.monitor;

public class MonitoredMethodExecution implements MonitoredExecution<ExecutionContext> {

	private final String methodSignature;
	private final MethodExecution methodExecution;

	public MonitoredMethodExecution(String methodSignature, MethodExecution methodExecution) {
		this.methodSignature = methodSignature;
		this.methodExecution = methodExecution;
	}

	@Override
	public String getInstanceName() {
		return null;
	}

	@Override
	public ExecutionContext createExecutionContext() {
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.setName(methodSignature);
		return executionContext;
	}

	@Override
	public Object execute() throws Exception {
		return methodExecution.execute();
	}

	@Override
	public void onPostExecute(ExecutionContext executionContext) {
	}

	/**
	 * In a Method execution context, we only want to monitor the topmost monitored (forwarding) method call.
	 * <p/>
	 * Example:<br/>
	 * Suppose, we have three methods: monitored1(), monitored2() and notMonitored().
	 * <pre><code>
	 * public void monitored1() {
	 *     monitored2();
	 * }
	 * public void monitored2() {
	 *     notMonitored();
	 * }
	 * public void notMonitored() {}
	 * </code></pre>
	 * The first two Methods are monitored with a {@link ExecutionContextMonitor}.<br/>
	 * If method1() is called, we only want to collect metrics for method1() and not for method2().<br/>
	 * If method2() is called, we want to collect metrics for that method.<br/>
	 * If notMonitored() is called directly, we don't want to collect metrics.
	 *
	 * @return false
	 */
	@Override
	public boolean isMonitorForwardedExecutions() {
		return false;
	}

	public interface MethodExecution {
		Object execute() throws Exception;
	}
}
