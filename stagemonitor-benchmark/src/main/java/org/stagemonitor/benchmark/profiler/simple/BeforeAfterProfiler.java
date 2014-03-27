package org.stagemonitor.benchmark.profiler.simple;

import org.stagemonitor.entities.profiler.CallStackElement;

public class BeforeAfterProfiler {

	static final ThreadLocal<CallStackElement> methodCallParent = new ThreadLocal<CallStackElement>();

	public static void start() {
		methodCallParent.set(new CallStackElement(methodCallParent.get()));
	}

	public static void stop(String signature) {
		CallStackElement currentStats = methodCallParent.get();
		currentStats.executionStopped(signature, System.nanoTime() - currentStats.getExecutionTime());
		methodCallParent.set(currentStats.getParent());
	}

	public static void setMethodCallRoot(CallStackElement root) {
		methodCallParent.set(root);
	}

}
