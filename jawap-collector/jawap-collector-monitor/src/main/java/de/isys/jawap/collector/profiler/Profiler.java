package de.isys.jawap.collector.profiler;

import de.isys.jawap.collector.core.JawapApplicationContext;
import de.isys.jawap.entities.profiler.CallStackElement;

public class Profiler {

	public static final long MIN_EXECUTION_TIME_NANOS = JawapApplicationContext.getConfiguration().getMinExecutionTimeNanos();

	private static final ThreadLocal<CallStackElement> methodCallParent = new ThreadLocal<CallStackElement>();

	public static void start() {
		final CallStackElement parent = methodCallParent.get();
		if (parent != null) {
			methodCallParent.set(new CallStackElement(parent));
		}
	}

	public static void stop(String signature) {
		final CallStackElement currentElement = methodCallParent.get();
		if (currentElement != null) {
			methodCallParent.set(currentElement.executionStopped(signature, System.nanoTime(), MIN_EXECUTION_TIME_NANOS));
		}
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
	public static CallStackElement activateProfiling() {
		CallStackElement root = new CallStackElement();
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
