package de.isys.jawap.entities.profiler;

import com.fasterxml.jackson.annotation.*;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.CascadeType.ALL;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CallStackElement {

	@Id
	@GeneratedValue
	private Integer id;
	@Transient
	public transient final long start = System.nanoTime();
	@ManyToOne(cascade = ALL)
	@JsonBackReference
	private CallStackElement parent;
	private String className;
	private String signature;
	private long executionTime;
	private long netExecutionTime;
	@OneToMany(cascade = ALL, mappedBy = "parent")
	@JsonManagedReference
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

	public void logStats(long totalExecutionTimeNs, int depth, StringBuilder log) {
		appendTimesPercentTable(totalExecutionTimeNs, log);
		appendCallTree(depth, log);
		preorderTraverseTreeAndComputeDepth(totalExecutionTimeNs, depth, log);
	}

	private void appendTimesPercentTable(long totalExecutionTimeNs, StringBuilder sb) {
		appendNumber(sb, getNetExecutionTime());
		appendPercent(sb, getNetExecutionTime(), totalExecutionTimeNs);

		appendNumber(sb, getExecutionTime());
		appendPercent(sb, getExecutionTime(), totalExecutionTimeNs);
	}

	private void appendNumber(StringBuilder sb, long time) {
		sb.append(String.format("%,9.2f", time / 1000000.0)).append("  ");
	}

	private void appendPercent(StringBuilder sb, long time, long totalExecutionTimeNs) {
		sb.append(String.format("%3.0f", time * 100 / (double) totalExecutionTimeNs)).append("%  ");
	}

	private void appendCallTree(int depth, StringBuilder sb) {
		for (int i = 1; i <= depth; i++) {
			if (i == depth) {
				if (isLastChild() && getChildren().isEmpty()) {
					sb.append("`-- ");
				} else {
					sb.append("+-- ");
				}
			} else {
				sb.append("|   ");
			}
		}
		sb.append(getSignature()).append('\n');
	}

	private boolean isLastChild() {
		final List<CallStackElement> parentChildren = getParent().getChildren();
		return parentChildren.get(parentChildren.size() - 1) == this;
	}

	private void preorderTraverseTreeAndComputeDepth(long totalExecutionTimeNs,
													 int depth, StringBuilder log) {
		for (CallStackElement callStats : getChildren()) {
			depth++;
			callStats.logStats(totalExecutionTimeNs, depth, log);
			depth--;
		}
	}
}