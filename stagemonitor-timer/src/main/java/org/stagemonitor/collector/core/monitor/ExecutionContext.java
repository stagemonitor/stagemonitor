package org.stagemonitor.collector.core.monitor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.stagemonitor.collector.core.MeasurementSession;
import org.stagemonitor.collector.profiler.CallStackElement;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class ExecutionContext {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	static {
		MAPPER.registerModule(new AfterburnerModule());
	}

	@JsonIgnore
	private String id = UUID.randomUUID().toString();
	private String name;
	@JsonIgnore
	private CallStackElement callStack;
	private long executionTime;
	private long cpuTime;
	private boolean error = false;
	@JsonProperty("@timestamp")
	private String timestamp;
	private String parameter;
	private String application;
	private String host;
	private String instance;
	private String exceptionMessage;
	private String stackTrace;

	public ExecutionContext() {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		df.setTimeZone(tz);
		this.timestamp = df.format(new Date());
	}

	public void setMeasurementSession(MeasurementSession measurementSession) {
		application = measurementSession.getApplicationName();
		host = measurementSession.getHostName();
		instance = measurementSession.getInstanceName();
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

	@JsonProperty("message")
	public String getCallStackAscii() {
		if (callStack == null) {
			return null;
		}
		return callStack.toString(true);
	}

	public String getCallStackJson() throws JsonProcessingException {
		if (callStack == null) {
			return null;
		}
		return MAPPER.writeValueAsString(callStack);
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

	public long getCpuTime() {
		return cpuTime;
	}

	public void setCpuTime(long cpuTime) {
		this.cpuTime = cpuTime;
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

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getInstance() {
		return instance;
	}

	public void setInstance(String instance) {
		this.instance = instance;
	}

	public String getExceptionMessage() {
		return exceptionMessage;
	}

	public void setExceptionMessage(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
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
