package org.stagemonitor.collector.core.monitor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.stagemonitor.collector.profiler.CallStackElement;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class ExecutionContext {

	@JsonIgnore
	private String id = UUID.randomUUID().toString();
	private String measurementSessionId;
	private String name;
	private CallStackElement callStack;
	// TODO cpu time
	private long executionTime;
	private boolean error = false;
	private String timestamp;
	private String parameter;

	public ExecutionContext() {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
		df.setTimeZone(tz);
		this.timestamp = df.format(new Date());
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean failed) {
		this.error = failed;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public CallStackElement getCallStack() {
		return callStack;
	}

	public void setCallStack(CallStackElement callStack) {
		this.callStack = callStack;
	}

	public String getMeasurementSessionId() {
		return measurementSessionId;
	}

	public void setMeasurementSessionId(String measurementSessionId) {
		this.measurementSessionId = measurementSessionId;
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

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getParameter() {
		return parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}

	public String getPlainText() {
		return toString();
	}

	@Override
	public String toString() {
		return toString(false);
	}

	public String toString(boolean asciiArt) {
		StringBuilder sb = new StringBuilder(3000);
		sb.append("id:     ").append(id).append('\n');
		sb.append("name:   ").append(name).append('\n');
		if (getParameter() != null) {
			sb.append("params: ").append(getParameter()).append('\n');
		}
		appendCallStack(sb, asciiArt);
		return sb.toString();
	}

	protected void appendCallStack(StringBuilder sb, boolean asciiArt) {
		if (getCallStack() != null) {
			sb.append("--------------------------------------------------------------------------\n");
			sb.append("Selftime (ms)                Total (ms)                   Method signature\n");
			sb.append("--------------------------------------------------------------------------\n");

			sb.append(getCallStack().toString(asciiArt));
		}
	}
}
