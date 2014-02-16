package de.isys.jawap.entities.profiler;

import com.fasterxml.jackson.annotation.*;

import javax.persistence.*;
import java.util.*;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CallStackElement {

	@Id
	@GeneratedValue
	private Integer id;
	@Lob
	@JsonIgnore
	private String callStackJson;

	@Transient
	@JsonBackReference
	private CallStackElement parent;
	@Transient
	private String signature;
	@Transient
	private long executionTime;
	@Transient
	@JsonManagedReference
	private List<CallStackElement> children = new LinkedList<CallStackElement>();

	public CallStackElement() {
	}

	public CallStackElement(CallStackElement parent) {
		this(parent, System.nanoTime());
	}

	public CallStackElement(CallStackElement parent, long startTimestamp) {
		this.parent = parent;
		executionTime = startTimestamp;
	}

	/**
	 * This static factory method also sets the parent-child relationships.
	 * Therefore it must perform a null check on parent, which costs about 5ns.
	 * @param parent the parent
	 * @return the newly created instance
	 */
	public static CallStackElement newInstance(CallStackElement parent) {
		CallStackElement cse = new CallStackElement();
		cse.setParent(parent);
		if (parent != null) {
			parent.getChildren().add(cse);
		}
		cse.setExecutionTime(System.nanoTime());
		return cse;
	}


	/**
	 * The execution time of the Method.
	 * Initially set to the start timestamp
	 *
	 * @return the execution time/start timestamp of the method
	 */
	public long getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}

	public long getNetExecutionTime() {
		long net = executionTime;
		for (CallStackElement child : children) {
			net -= child.executionTime;
		}

		return net;
	}

	public List<CallStackElement> getChildren() {
		return children;
	}

	public void setChildren(List<CallStackElement> children) {
		this.children = children;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public CallStackElement getParent() {
		return parent;
	}

	public void setParent(CallStackElement parent) {
		this.parent = parent;
	}

	public String getCallStackJson() {
		return callStackJson;
	}

	public void setCallStackJson(String callStackJson) {
		this.callStackJson = callStackJson;
	}

	public void profile(String signature, long executionTime) {
		this.signature = signature;
		this.executionTime = executionTime;
	}

	@Override
	public String toString() {
		return toString(false);
	}

	public String toString(boolean asciiArt) {
		final StringBuilder sb = new StringBuilder(3000);
		logStats(getExecutionTime(), new Stack<String>(), sb, asciiArt);
		return sb.toString();
	}

	public void logStats(long totalExecutionTimeNs, Stack<String> indentationStack, StringBuilder log,
						 final boolean asciiArt) {
		appendTimesPercentTable(totalExecutionTimeNs, log);
		appendCallTree(indentationStack, log, asciiArt);

		for (CallStackElement callStats : getChildren()) {
			if (!isRoot()) {
				if (isLastChild()) {
					indentationStack.push("    ");
				} else {
					indentationStack.push(asciiArt ? "│   " : "|   ");
				}
			}
			callStats.logStats(totalExecutionTimeNs, indentationStack, log, asciiArt);
			if (!isRoot()) indentationStack.pop();
		}
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

	private void appendCallTree(Stack<String> indentationStack, StringBuilder sb, final boolean asciiArt) {
		for (String indentation : indentationStack) {
			sb.append(indentation);
		}
		if (!isRoot()) {
			if (isLastChild()) {
				sb.append(asciiArt ? "└── " : "`-- ");
			} else {
				sb.append(asciiArt ? "├── " : "|-- ");
			}
		}

		sb.append(getSignature()).append('\n');
	}

	private boolean isLastChild() {
		final CallStackElement parent = getParent();
		if (parent == null) return true;
		final List<CallStackElement> parentChildren = parent.getChildren();
		return parentChildren.get(parentChildren.size() - 1) == this;
	}

	private boolean isRoot() {
		return parent == null;
	}

	public static void main(String[] args) {
		CallStackElement c0 = new CallStackElement();
		CallStackElement c1_1 = new CallStackElement(c0);
		CallStackElement c2_1 = new CallStackElement(c1_1);
		CallStackElement c2_2 = new CallStackElement(c1_1);
		c1_1.setChildren(Arrays.asList(c2_1, c2_2));
		CallStackElement c1_2 = new CallStackElement(c0);
		c0.setChildren(Arrays.asList(c1_1, c1_2));
		System.out.println(c0);
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}