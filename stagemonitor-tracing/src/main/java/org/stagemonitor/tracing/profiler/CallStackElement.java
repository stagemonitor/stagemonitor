package org.stagemonitor.tracing.profiler;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.tracing.TracingPlugin;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class CallStackElement {

	private static final boolean useObjectPooling = Stagemonitor.getPlugin(TracingPlugin.class).isProfilerObjectPoolingActive();
	private static Queue<CallStackElement> objectPool;
	static {
		if (useObjectPooling) {
			objectPool = new ArrayBlockingQueue<CallStackElement>(100000);
		}
	}
	private static final String HORIZONTAL;
	private static final String HORIZONTAL_ANGLE;
	private static final String ANGLE;
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

	public static CallStackElement createRoot(String signature) {
		return CallStackElement.create(null, signature, System.nanoTime());
	}

	public static CallStackElement create(CallStackElement parent, String signature) {
		return CallStackElement.create(parent, signature, System.nanoTime());
	}

	/**
	 * This static factory method also sets the parent-child relationships.
	 * @param parent the parent
	 * @param startTimestamp the timestamp at the beginning of the method
	 */
	public static CallStackElement create(CallStackElement parent, String signature, long startTimestamp) {
		CallStackElement cse;
		if (useObjectPooling) {
			cse = objectPool.poll();
			if (cse == null) {
				cse = new CallStackElement();
			}
		} else {
			cse = new CallStackElement();
		}

		cse.executionTime = startTimestamp;
		cse.signature = signature;
		if (parent != null) {
			cse.parent = parent;
			parent.children.add(cse);
		}
		return cse;
	}

	public void recycle() {
		if (!useObjectPooling) {
			return;
		}
		parent = null;
		signature = null;
		executionTime = 0;
		for (CallStackElement child : children) {
			child.recycle();
		}
		children.clear();
		objectPool.offer(this);
	}

	public void removeCallsFasterThan(long thresholdNs) {
		for (Iterator<CallStackElement> iterator = children.iterator(); iterator.hasNext(); ) {
			CallStackElement child = iterator.next();
			if (child.executionTime < thresholdNs && !child.isIOQuery()) {
				iterator.remove();
				child.recycle();
			} else {
				child.removeCallsFasterThan(thresholdNs);
			}
		}
	}

	public boolean isIOQuery() {
		// that might be a bit ugly, but it saves reference to a boolean and thus memory
		return signature.charAt(signature.length() - 1) == ' ';
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

	public void incrementExecutionTime(long additionalExecutionTime) {
		executionTime += additionalExecutionTime;
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

	/**
	 * Returns <code>null</code>, if the signature is no method signature (such as 'total') ClassName#methodName
	 * otherwise
	 *
	 * @return <code>null</code>, if the signature is no method signature (such as 'total') ClassName#methodName
	 * otherwise
	 */
	public String getShortSignature() {
		if (signature.indexOf('(') == -1 || signature.indexOf(':') != -1) {
			return null;
		}
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
		children.remove(children.size() - 1).recycle();
	}

	/**
	 * Removes this node from the parent
	 */
	public void remove() {
		if (parent != null) {
			parent.getChildren().remove(this);
			recycle();
		}
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
		this.executionTime = localExecutionTime;
		if (localExecutionTime < minExecutionTime && parent != null) {
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
					indentationStack.add("    ");
				} else {
					indentationStack.add(asciiArt ? HORIZONTAL : "|   ");
				}
			}
			callStats.logStats(totalExecutionTimeNs, indentationStack, sb, asciiArt);
			if (!isRoot()) {
				indentationStack.pollLast();
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
				sb.append(asciiArt ? (char) 9608 : '|');
			} else if (i == actualBars && includeHalfBarAtEnd) {
				sb.append(asciiArt ? (char) 9619 : ':');
			} else {
				sb.append(asciiArt ? (char) 9617 : '-');
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

		final String shortSignature = getShortSignature();
		if (shortSignature != null) {
			sb.append(shortSignature);
		} else {
			sb.append(getSignature());
		}
		sb.append('\n');
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
