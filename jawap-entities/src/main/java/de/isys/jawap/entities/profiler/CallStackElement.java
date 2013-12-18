package de.isys.jawap.entities.profiler;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

@Entity
public class CallStackElement {

	@Id
	@GeneratedValue
	private Integer id;
	@Transient
	public transient final long start = System.nanoTime();
	@ManyToOne
	private CallStackElement parent;
	private String className;
	private String signature;
	private long executionTime;
	private long netExecutionTime;
	private long timestamp = System.currentTimeMillis();
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "parent")
	private List<CallStackElement> children = new ArrayList<CallStackElement>();

	public CallStackElement() {
	}

	public CallStackElement(CallStackElement parent) {
		this.parent = parent;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public long getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}

	public long getNetExecutionTime() {
		return netExecutionTime;
	}

	public void setNetExecutionTime(long netExecutionTime) {
		this.netExecutionTime = netExecutionTime;
	}

	public List<CallStackElement> getChildren() {
		return children;
	}

	public void setChildren(List<CallStackElement> children) {
		this.children = children;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void addToNetExecutionTime(long executionTime) {
		netExecutionTime += executionTime;
	}

	public void subtractFromNetExecutionTime(long executionTime) {
		netExecutionTime -= executionTime;
	}

	public CallStackElement getParent() {
		return parent;
	}

	public void setParent(CallStackElement parent) {
		this.parent = parent;
	}
}