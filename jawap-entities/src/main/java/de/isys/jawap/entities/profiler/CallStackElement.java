package de.isys.jawap.entities.profiler;

import com.fasterxml.jackson.annotation.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.*;

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
	private List<CallStackElement> children = new LinkedList<CallStackElement>();

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

	@Override
	public String toString() {
		return toString(true);
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
				} else  {
					indentationStack.push(asciiArt ? "│   ": "|   ");
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
				sb.append(asciiArt ? "└── " : "`--");
			} else {
				sb.append(asciiArt ? "├── ": "|--");
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
}