package de.isys.jawap.collector.profile;

import de.isys.jawap.collector.model.HttpRequestStats;
import de.isys.jawap.collector.model.MethodCallStats;

public class Profiler {

	public static final long MIN_EXECUTION_TIME_NANOS = 1000000L; // TODO make configurable

	private static final ThreadLocal<MethodCallStats> methodCallParent = new ThreadLocal<MethodCallStats>();

	private static final ThreadLocal<HttpRequestStats> currentRequestStats = new ThreadLocal<HttpRequestStats>();


	public static MethodCallStats start(MethodCallStats parent) {
		MethodCallStats stats = new MethodCallStats(parent);
		parent.getChildren().add(stats);
		methodCallParent.set(stats);
		return stats;
	}

	public static void stop(MethodCallStats currentStats, String targetClass, String methodName) {
		long executionTime = System.nanoTime() - currentStats.start;

		if (executionTime >= MIN_EXECUTION_TIME_NANOS) {
			fillMethodCallStats(targetClass, methodName, currentStats, executionTime);
		} else {
			currentStats.parent.getChildren().remove(currentStats); // TODO remove on ArrayList is 0(n)
		}
		methodCallParent.set(currentStats.parent);
		currentStats.parent.subtractFromNetExecutionTime(executionTime);
	}

	private static void fillMethodCallStats(String targetClass, String methodName, MethodCallStats stats, long executionTime) {
		stats.setRequestStats(currentRequestStats.get());
		stats.setMethodName(methodName);
		stats.setClassName(targetClass);
		stats.setExecutionTime(executionTime);
		stats.addToNetExecutionTime(executionTime);
	}

	public static MethodCallStats getMethodCallParent() {
		return methodCallParent.get();
	}

	public static void clearStats() {
		methodCallParent.remove();
	}


	public static void setMethodCallRoot(MethodCallStats root) {
		methodCallParent.set(root);
	}

	public static void setCurrentRequestStats(HttpRequestStats requestStats) {
		currentRequestStats.set(requestStats);
	}

	public static void clearCurrentRequestStats() {
		currentRequestStats.remove();
	}

	public static void clearAllThreadLoals() {
		clearStats();
		clearCurrentRequestStats();
	}
}
