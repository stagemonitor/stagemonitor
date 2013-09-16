package de.isys.jawap.collector.profile;

import de.isys.jawap.collector.model.HttpRequestStats;
import de.isys.jawap.collector.model.MethodCallStats;

public class Profiler {

	public static final long MIN_EXECUTION_TIME_NANOS = /*100000*/0L; // TODO make configurable

	private static final ThreadLocal<MethodCallStats> methodCallParent = new ThreadLocal<MethodCallStats>();

	private static final ThreadLocal<HttpRequestStats> currentRequestStats = new ThreadLocal<HttpRequestStats>();


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
			// Only profile if in an HTTP request context
			HttpRequestStats httpRequestStats = currentRequestStats.get();
			if (httpRequestStats != null) {
				MethodCallStats stats = new MethodCallStats(parent);
				httpRequestStats.setMethodCallStats(stats);
				methodCallParent.set(stats);
			}
		}
	}

	public static void stop(String targetClass, String methodName) {
		MethodCallStats currentStats = methodCallParent.get();
		if (currentStats != null) {
			long executionTime = System.nanoTime() - currentStats.start;

			final MethodCallStats parent = currentStats.parent;
			if (executionTime >= MIN_EXECUTION_TIME_NANOS) {
				fillMethodCallStats(targetClass, methodName, currentStats, executionTime);
			} else if (parent != null) {
				parent.getChildren().remove(currentStats); // TODO remove on ArrayList is 0(n)
			}

			if (parent == null) {
				clearAllThreadLoals();
			} else {
				parent.subtractFromNetExecutionTime(executionTime);
				methodCallParent.set(parent);
			}
		}
	}

	private static void fillMethodCallStats(String targetClass, String methodName, MethodCallStats stats, long executionTime) {
		stats.setMethodName(methodName);
		stats.setClassName(targetClass);
		stats.setExecutionTime(executionTime);
		stats.addToNetExecutionTime(executionTime);
	}

	/**
	 * Sets the current HTTP requests stats
	 * This serves as a hint for the Profiler, that Method metrics should be gathered.
	 *
	 * @param requestStats the current HTTP requests stats
	 */
	public static void setCurrentRequestStats(HttpRequestStats requestStats) {
		currentRequestStats.set(requestStats);
	}

	private static void clearAllThreadLoals() {
		methodCallParent.remove();
		currentRequestStats.remove();
	}
}
