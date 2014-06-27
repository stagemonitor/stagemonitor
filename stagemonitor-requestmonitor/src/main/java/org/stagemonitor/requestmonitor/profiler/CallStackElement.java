package org.stagemonitor.requestmonitor.profiler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CallStackElement {

	private static final String horizontal;      // '│   '
	private static final String angle;           // '└── '
	private static final String horizontalAngle; // '├── '
	static {
		horizontal = new String(new char[]{9474, ' ', ' ', ' '});
		angle = new String(new char[] {9492, 9472, 9472, ' '});
		horizontalAngle = new String(new char[]{9500, 9472, 9472, ' '});
	}

	@JsonIgnore
	private CallStackElement parent;
	private String signature;
	private long executionTime;
	private List<CallStackElement> children = new LinkedList<CallStackElement>();

	public CallStackElement() {
		this(null);
	}

	public CallStackElement(CallStackElement parent) {
		this(parent, System.nanoTime());
	}

	/**
	 * This static factory method also sets the parent-child relationships.
	 * @param parent the parent
	 * @param startTimestamp the timestamp at the beginning of the method
	 */
	public CallStackElement(CallStackElement parent, long startTimestamp) {
		executionTime = startTimestamp;
		if (parent != null) {
			this.parent = parent;
			parent.getChildren().add(this);
		}
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

	private void removeLastChild() {
		children.remove(children.size() - 1);
	}

	public void executionStopped(String signature, long executionTime) {
		this.signature = signature;
		this.executionTime = executionTime;
	}

	/**
	 *
	 * @param signature the signature of the profiled method
	 * @param timestamp the stop timestamp
	 * @param minExecutionTime the threshold for the minimum execution time
	 * @return the parent of this {@link CallStackElement}
	 */
	public CallStackElement executionStopped(String signature, long timestamp, long minExecutionTime) {
		long executionTime = timestamp - this.executionTime; // executionTime is initialized to start timestamp
		if (executionTime >= minExecutionTime) {
			this.signature = signature;
			this.executionTime = executionTime;
		} else if (parent != null) {
			// <this> is always the last entry in parent.getChildren()
			parent.removeLastChild();
		}
		return parent;
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
		appendTimesPercentTable(totalExecutionTimeNs, log, asciiArt);
		appendCallTree(indentationStack, log, asciiArt);

		for (CallStackElement callStats : getChildren()) {
			if (!isRoot()) {
				if (isLastChild()) {
					indentationStack.push("    ");
				} else {
					indentationStack.push(asciiArt ? horizontal : "|   ");
				}
			}
			callStats.logStats(totalExecutionTimeNs, indentationStack, log, asciiArt);
			if (!isRoot()) indentationStack.pop();
		}
	}

	private void appendTimesPercentTable(long totalExecutionTimeNs, StringBuilder sb, boolean asciiArt) {
		appendNumber(sb, getNetExecutionTime());
		appendPercent(sb, getNetExecutionTime(), totalExecutionTimeNs, asciiArt);

		appendNumber(sb, getExecutionTime());
		appendPercent(sb, getExecutionTime(), totalExecutionTimeNs, asciiArt);
	}

	private void appendNumber(StringBuilder sb, long time) {
		sb.append(String.format(Locale.US, "%09.2f", time / 1000000.0)).append("  ");
	}

	private void appendPercent(StringBuilder sb, long time, long totalExecutionTimeNs, boolean asciiArt) {
		final double percent = time / (double) totalExecutionTimeNs;
		sb.append(String.format(Locale.US, "%03.0f", percent * 100)).append("% ").append(printPercentAsBar(percent, 10, asciiArt)).append(' ');
	}

	static String printPercentAsBar(double percent, int totalBars, boolean asciiArt) {
		int actualBars = (int) (percent * totalBars);
		boolean includeHalfBarAtEnd = actualBars * 2 != (int) (percent * totalBars * 2);
		StringBuilder sb = new StringBuilder(totalBars);
		for (int i = 0; i < totalBars; i++) {
			if (i < actualBars) {
				sb.append(asciiArt ? (char) 9608 : '|'); // █
			} else if (i == actualBars && includeHalfBarAtEnd) {
				sb.append(asciiArt ? (char) 9619 : ':'); // ▓
			} else {
				sb.append(asciiArt ? (char) 9617 : ' '); // ▒
			}
		}
		return sb.toString();
	}

	private void appendCallTree(Stack<String> indentationStack, StringBuilder sb, final boolean asciiArt) {
		for (String indentation : indentationStack) {
			sb.append(indentation);
		}
		if (!isRoot()) {
			if (isLastChild()) {
				sb.append(asciiArt ? angle : "`-- ");
			} else {
				sb.append(asciiArt ? horizontalAngle : "|-- ");
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
}