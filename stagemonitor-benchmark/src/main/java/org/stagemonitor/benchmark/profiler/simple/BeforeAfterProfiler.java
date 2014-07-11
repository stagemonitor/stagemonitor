package org.stagemonitor.benchmark.profiler.simple;

import org.stagemonitor.requestmonitor.profiler.CallStackElement;

public class BeforeAfterProfiler {

	static final ThreadLocal<CallStackElement> methodCallParent = new ThreadLocal<CallStackElement>();

	public static void start(String signature) {
		methodCallParent.set(new CallStackElement(methodCallParent.get(), signature));
	}

	public static void stop() {
		CallStackElement currentStats = methodCallParent.get();
		currentStats.executionStopped(System.nanoTime() - currentStats.getExecutionTime());
		methodCallParent.set(currentStats.getParent());
	}

	public static void setMethodCallRoot(CallStackElement root) {
		methodCallParent.set(root);
	}

}
