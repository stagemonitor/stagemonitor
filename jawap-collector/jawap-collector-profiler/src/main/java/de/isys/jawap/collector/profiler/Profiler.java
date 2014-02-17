package de.isys.jawap.collector.profiler;

import de.isys.jawap.collector.core.JawapApplicationContext;
import de.isys.jawap.entities.profiler.CallStackElement;

public class Profiler {

	public static final long MIN_EXECUTION_TIME_NANOS = JawapApplicationContext.getConfiguration().getMinExecutionTimeNanos();

	private static final ThreadLocal<CallStackElement> methodCallParent = new ThreadLocal<CallStackElement>();

	public static boolean isProfilingActive() {
		return methodCallParent.get() != null;
	}

	/**
	 * Starts the profiling of a method
	 */
	public static void start() {
		CallStackElement parent = methodCallParent.get();
		if (parent != null) {
			methodCallParent.set(new CallStackElement(parent));
		}
	}

	public static void stop(String signature) {
		methodCallParent.set(methodCallParent.get().profile2(signature, System.nanoTime()));
	}

	/**
	 * Activates the profiling by setting the provided {@link CallStackElement} as the root
	 *
	 * @param root the root of the call stack
	 */
	public static void activateProfiling(CallStackElement root) {
		methodCallParent.set(root);
	}

	public static CallStackElement getMethodCallParent() {
		return methodCallParent.get();
	}

	public static void deactivateProfiling() {
		methodCallParent.set(null);
	}

	public static void clearMethodCallParent() {
		methodCallParent.remove();
	}
}
