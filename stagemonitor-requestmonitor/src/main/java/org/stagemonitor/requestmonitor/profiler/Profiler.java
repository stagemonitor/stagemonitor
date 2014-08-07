package org.stagemonitor.requestmonitor.profiler;

import org.stagemonitor.core.StageMonitor;

public final class Profiler {

	public static final long MIN_EXECUTION_TIME_NANOS = StageMonitor.getConfiguration().getMinExecutionTimeNanos();

	private static final ThreadLocal<CallStackElement> methodCallParent = new ThreadLocal<CallStackElement>();

	private Profiler() {
	}

	public static void start(String signature) {
		final CallStackElement parent = methodCallParent.get();
		if (parent != null) {
			methodCallParent.set(new CallStackElement(parent, signature));
		}
	}

	public static void stop() {
		final CallStackElement currentElement = methodCallParent.get();
		if (currentElement != null) {
			methodCallParent.set(currentElement.executionStopped(System.nanoTime(), MIN_EXECUTION_TIME_NANOS));
		}
	}

	public static void addCall(String signature, long executionTimeNanos) {
		final CallStackElement currentCall = methodCallParent.get();
		new CallStackElement(currentCall, signature, executionTimeNanos);
	}

	public static boolean isProfilingActive() {
		return methodCallParent.get() != null;
	}

	/**
	 * Activates the profiling for the current thread by setting the provided
	 * {@link CallStackElement} as the root of the call stack
	 *
	 * @return the root of the call stack
	 */
	public static CallStackElement activateProfiling(String signature) {
		CallStackElement root = new CallStackElement(signature);
		methodCallParent.set(root);
		return root;
	}

	public static void deactivateProfiling() {
		methodCallParent.set(null);
	}

	public static CallStackElement getMethodCallParent() {
		return methodCallParent.get();
	}

	public static void clearMethodCallParent() {
		methodCallParent.remove();
	}
}
