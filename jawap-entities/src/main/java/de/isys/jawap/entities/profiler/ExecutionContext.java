package de.isys.jawap.entities.profiler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.isys.jawap.entities.MeasurementSession;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.io.IOException;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.LAZY;

@MappedSuperclass
public class ExecutionContext {

	public static final ObjectMapper MAPPER = new ObjectMapper();
	@Id
	@GeneratedValue
	private Integer id;
	@ManyToOne(fetch = LAZY, cascade = ALL)
	@JsonIgnore
	private MeasurementSession measurementSession;
	private String name;
	@OneToOne(fetch = LAZY, cascade = ALL)
	@JsonIgnore
	private CallStackLob callStackLob;
	@Transient
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

	public CallStackLob getCallStackLob() {
		return callStackLob;
	}

	public void setCallStackLob(CallStackLob callStackLob) {
		this.callStackLob = callStackLob;
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

	public void convertCallStackToLob() {
		if (callStack != null) {
			try {
				callStackLob = new CallStackLob();
				callStackLob.setCallStackJson(MAPPER.writeValueAsString(callStack));
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void convertLobToCallStack() {
		if (callStackLob != null) {
			try {
				callStack = MAPPER.readValue(callStackLob.getCallStackJson(), CallStackElement.class);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
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
