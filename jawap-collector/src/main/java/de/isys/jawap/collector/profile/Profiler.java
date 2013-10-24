package de.isys.jawap.collector.profile;

import de.isys.jawap.collector.model.ExecutionContext;
import de.isys.jawap.collector.model.MethodCallStats;

public class Profiler {

	public static final long MIN_EXECUTION_TIME_NANOS = /*100000*/0L; // TODO make configurable

	private static final ThreadLocal<MethodCallStats> methodCallParent = new ThreadLocal<MethodCallStats>();

	private static final ThreadLocal<ExecutionContext> executionContext = new ThreadLocal<ExecutionContext>();

	/**
	 * @return true, if a executionContext is set, false otherwise
	 * @see #setExecutionContext(de.isys.jawap.collector.model.ExecutionContext)
	 */
	public static boolean isProfilingActive() {
		return executionContext.get() != null;
	}

	/**
	 * Starts the profiling of a method
	 */
	public static void start() {
		MethodCallStats parent = methodCallParent.get();
		if (parent != null) {
			MethodCallStats stats = new MethodCallStats(parent);
			parent.getChildren().add(stats);
			methodCallParent.set(stats);
		} else {
			// profile only if we are in a execution context
			ExecutionContext executionContext = Profiler.executionContext.get();
			if (executionContext != null) {
				MethodCallStats root = new MethodCallStats(null);
				executionContext.setMethodCallStats(root);
				methodCallParent.set(root);
			}
		}
	}

	public static void stop(String targetClass, String signature) {
		MethodCallStats currentStats = methodCallParent.get();
		if (currentStats != null) {
			long executionTime = System.nanoTime() - currentStats.start;

			final MethodCallStats parent = currentStats.parent;
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
	 * This serves as a hint for the Profiler, that Method metrics should be gathered.
	 * Those metrics are added to the {@link ExecutionContext}
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
