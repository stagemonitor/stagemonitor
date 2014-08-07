package org.stagemonitor.requestmonitor.profiler;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class CallStackElement {

	private static final String HORIZONTAL;       // '│   '
	private static final String ANGLE;            // '└── '
	private static final String HORIZONTAL_ANGLE; // '├── '
	static {
		HORIZONTAL = new String(new char[]{9474, ' ', ' ', ' '});
		ANGLE = new String(new char[] {9492, 9472, 9472, ' '});
		HORIZONTAL_ANGLE = new String(new char[]{9500, 9472, 9472, ' '});
	}

	@JsonIgnore
	private CallStackElement parent;
	private String signature;
	private long executionTime;
	private List<CallStackElement> children = new LinkedList<CallStackElement>();

	public CallStackElement(String signature) {
		this(null, signature);
	}

	public CallStackElement(CallStackElement parent, String signature) {
		this(parent, signature, System.nanoTime());
	}

	/**
	 * This static factory method also sets the parent-child relationships.
	 * @param parent the parent
	 * @param startTimestamp the timestamp at the beginning of the method
	 */
	public CallStackElement(CallStackElement parent, String signature, long startTimestamp) {
		executionTime = startTimestamp;
		this.signature = signature;
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

	@JsonIgnore
	public String getShortSignature() {
		String[] split = signature.substring(0, signature.indexOf('(')).split("\\.");
		if (split.length > 1) {
			return split[split.length - 2] + '#' + split[split.length - 1];
		} else {
			return split.length == 1 ? split[0] : "null";
		}
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

	public void executionStopped(long executionTime) {
		this.executionTime = executionTime;
	}

	/**
	 *
	 * @param timestamp the stop timestamp
	 * @param minExecutionTime the threshold for the minimum execution time
	 * @return the parent of this {@link CallStackElement}
	 */
	public CallStackElement executionStopped(long timestamp, long minExecutionTime) {
		// executionTime is initialized to start timestamp
		long localExecutionTime = timestamp - this.executionTime;
		if (localExecutionTime >= minExecutionTime) {
			this.executionTime = localExecutionTime;
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
		logStats(getExecutionTime(), new LinkedList<String>(), sb, asciiArt);
		return sb.toString();
	}

	public void logStats(long totalExecutionTimeNs, Deque<String> indentationStack, StringBuilder sb,
						 final boolean asciiArt) {
		if (isRoot()) {
			sb.append("----------------------------------------------------------------------\n");
			sb.append("Selftime (ms)              Total (ms)                 Method signature\n");
			sb.append("----------------------------------------------------------------------\n");
		}
		appendTimesPercentTable(totalExecutionTimeNs, sb, asciiArt);
		appendCallTree(indentationStack, sb, asciiArt);

		for (CallStackElement callStats : getChildren()) {
			if (!isRoot()) {
				if (isLastChild()) {
					indentationStack.push("    ");
				} else {
					indentationStack.push(asciiArt ? HORIZONTAL : "|   ");
				}
			}
			callStats.logStats(totalExecutionTimeNs, indentationStack, sb, asciiArt);
			if (!isRoot()) {
				indentationStack.pop();
			}
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
				sb.append(asciiArt ? (char) 9617 : '-'); // ▒
			}
		}
		return sb.toString();
	}

	private void appendCallTree(Deque<String> indentationStack, StringBuilder sb, final boolean asciiArt) {
		for (String indentation : indentationStack) {
			sb.append(indentation);
		}
		if (!isRoot()) {
			if (isLastChild()) {
				sb.append(asciiArt ? ANGLE : "`-- ");
			} else {
				sb.append(asciiArt ? HORIZONTAL_ANGLE : "|-- ");
			}
		}

		sb.append(getSignature()).append('\n');
	}

	private boolean isLastChild() {
		if (parent == null) {
			return true;
		}
		final List<CallStackElement> parentChildren = parent.getChildren();
		return parentChildren.get(parentChildren.size() - 1) == this;
	}

	private boolean isRoot() {
		return parent == null;
	}
}
