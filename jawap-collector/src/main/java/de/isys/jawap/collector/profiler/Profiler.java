package de.isys.jawap.collector.profiler;

import de.isys.jawap.collector.core.ApplicationContext;
import de.isys.jawap.entities.profiler.CallStackElement;
import de.isys.jawap.entities.profiler.ExecutionContext;

public class Profiler {

	public static final long MIN_EXECUTION_TIME_NANOS = ApplicationContext.getConfiguration().getMinExecutionTimeNanos();

	private static final ThreadLocal<CallStackElement> methodCallParent = new ThreadLocal<CallStackElement>();

	private static final ThreadLocal<ExecutionContext> executionContext = new ThreadLocal<ExecutionContext>();

	/**
	 * @return true, if a executionContext is set, false otherwise
	 * @see #setExecutionContext(ExecutionContext)
	 */
	public static boolean isProfilingActive() {
		return executionContext.get() != null;
	}

	/**
	 * Starts the profiling of a method
	 */
	public static void start() {
		CallStackElement parent = methodCallParent.get();
		if (parent != null) {
			CallStackElement stats = new CallStackElement(parent);
			parent.getChildren().add(stats);
			methodCallParent.set(stats);
		} else {
			// profile only if we are in a execution context
			ExecutionContext executionContext = Profiler.executionContext.get();
			if (executionContext != null) {
				CallStackElement root = new CallStackElement(null);
				executionContext.setCallStack(root);
				methodCallParent.set(root);
			}
		}
	}

	public static void stop(String targetClass, String signature) {
		CallStackElement currentStats = methodCallParent.get();
		if (currentStats != null) {
			long executionTime = System.nanoTime() - currentStats.start;

			final CallStackElement parent = currentStats.getParent();
			if (executionTime >= MIN_EXECUTION_TIME_NANOS) {
				currentStats.setSignature(signature);
				currentStats.setClassName(targetClass);
				currentStats.setExecutionTime(executionTime);
				currentStats.addToNetExecutionTime(executionTime);
			} else if (parent != null) {
				// currentStats is always the last entry in parent.getChildren()
				parent.getChildren().remove(parent.getChildren().size() - 1);
			}

			if (parent == null) {
				clearAllThreadLoals();
			} else {
				parent.subtractFromNetExecutionTime(executionTime);
				methodCallParent.set(parent);
			}
		}
	}

	/**
	 * Sets the current {@link ExecutionContext}
	 * This serves as a hint for the Profiler, that a Call Stack should be gathered.
	 * The Call Stack is added to the {@link ExecutionContext}
	 *
	 * @param executionContext the current HTTP requests stats
	 */
	public static void setExecutionContext(ExecutionContext executionContext) {
		Profiler.executionContext.set(executionContext);
	}

	private static void clearAllThreadLoals() {
		methodCallParent.remove();
		executionContext.remove();
	}
}
