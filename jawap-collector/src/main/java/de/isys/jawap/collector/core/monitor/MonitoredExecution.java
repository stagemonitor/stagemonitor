package de.isys.jawap.collector.core.monitor;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import de.isys.jawap.entities.profiler.ExecutionContext;

public abstract class MonitoredExecution<T extends ExecutionContext> {

	protected MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate("request");

	public String getInstanceName() {
		return null;
	}

	public abstract String getRequestName();

	public abstract String getTimerName(String requestName);

	public abstract T getExecutionContext();

	public abstract void execute() throws Exception;

	public abstract void onPostExecute(T executionContext);

}
