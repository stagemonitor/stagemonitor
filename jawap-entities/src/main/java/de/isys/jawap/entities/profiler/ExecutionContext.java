package de.isys.jawap.entities.profiler;

import de.isys.jawap.entities.MeasurementSession;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.EAGER;

@MappedSuperclass
public class ExecutionContext {

	@Id
	@GeneratedValue
	private Integer id;
	@ManyToOne(fetch = EAGER, cascade = ALL)
	private MeasurementSession measurementSession;
	private String name;
	@OneToOne(cascade = ALL)
	private CallStackElement callStack;
	private long executionTime;

	public boolean isFailed() {
		return failed;
	}

	public void setFailed(boolean failed) {
		this.failed = failed;
	}

	private boolean failed = false;

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
}
