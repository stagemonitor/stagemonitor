package de.isys.jawap.collector.model;

public class GenericExecutionContext implements ExecutionContext {

	private MethodCallStats methodCallStats;

	public MethodCallStats getMethodCallStats() {
		return methodCallStats;
	}

	@Override
	public void setMethodCallStats(MethodCallStats methodCallStats) {
		this.methodCallStats = methodCallStats;
	}
}
