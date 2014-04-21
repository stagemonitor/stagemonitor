package org.stagemonitor.collector.core.monitor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.stagemonitor.collector.core.MeasurementSession;
import org.stagemonitor.collector.profiler.CallStackElement;

public class ExecutionContext {

	private Integer id;
	@JsonIgnore
	private MeasurementSession measurementSession;
	private String name;
	private CallStackElement callStack;
	// TODO cpu time
	private long executionTime;
	private boolean error = false;
	private long timestamp = System.currentTimeMillis();
	private String parameter;

	public boolean isError() {
		return error;
	}

	public void setError(boolean failed) {
		this.error = failed;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public CallStackElement getCallStack() {
		return callStack;
	}

	public void setCallStack(CallStackElement callStack) {
		this.callStack = callStack;
	}

	public MeasurementSession getMeasurementSession() {
		return measurementSession;
	}

	public void setMeasurementSession(MeasurementSession measurementSession) {
		this.measurementSession = measurementSession;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getParameter() {
		return parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}

	@Override
	public String toString() {
		return toString(false);
	}

	public String toString(boolean asciiArt) {
		StringBuilder sb = new StringBuilder(3000);
		sb.append(name);
		if (getParameter() != null) {
			sb.append(getParameter());
		}
		appendCallStack(sb, asciiArt);
		return sb.toString();
	}

	protected void appendCallStack(StringBuilder sb, boolean asciiArt) {
		if (getCallStack() != null) {
			sb.append("--------------------------------------------------\n");
			sb.append("Selftime (ms)    Total (ms)       Method signature\n");
			sb.append("--------------------------------------------------\n");

			sb.append(getCallStack().toString(asciiArt));
		}
	}
}
