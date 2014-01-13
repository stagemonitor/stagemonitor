package de.isys.jawap.entities.profiler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.isys.jawap.entities.MeasurementSession;

import javax.persistence.*;

import java.io.IOException;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.LAZY;

@MappedSuperclass
// TODO Name vereinheitlichen
public class ExecutionContext {

	@Id
	@GeneratedValue
	private Integer id;
	@ManyToOne(fetch = LAZY, cascade = ALL)
	@JsonIgnore
	private MeasurementSession measurementSession;
	private String name;
	@OneToOne(fetch = LAZY, cascade = ALL)
	private CallStackElement callStack;
	private long executionTime;
	private boolean error = false;

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

	public void setCallStack(CallStackElement callStackElement) {
		this.callStack = callStackElement;
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

	public void convertCallStackToJson() {
		if (callStack != null) {
			try {
				callStack.setCallStackJson(new ObjectMapper().writeValueAsString(callStack));
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void convertJsonToCallStack() {
		if (callStack != null) {
			try {
				callStack = new ObjectMapper().readValue(callStack.getCallStackJson(), CallStackElement.class);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
