package de.isys.jawap.collector.core.monitor;

import com.codahale.metrics.MetricRegistry;
import de.isys.jawap.collector.core.ApplicationContext;
import de.isys.jawap.entities.profiler.ExecutionContext;

public abstract class MonitoredExecution<T extends ExecutionContext> {

	protected final MetricRegistry metricRegistry;

	protected MonitoredExecution() {
		this.metricRegistry = ApplicationContext.getMetricRegistry();
	}

	protected MonitoredExecution(MetricRegistry metricRegistry) {
		this.metricRegistry = metricRegistry;
	}

	public String getInstanceName() {
		return null;
	}

	public abstract String getRequestName();

	public abstract T getExecutionContext();

	public abstract void execute() throws Exception;

	public abstract void onPostExecute(T executionContext);

}
