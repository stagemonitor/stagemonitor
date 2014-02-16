package de.isys.jawap.benchmark.profiler.simple;

import de.isys.jawap.entities.profiler.CallStackElement;

public class BeforeAfterProfiler {

	static final ThreadLocal<CallStackElement> methodCallParent = new ThreadLocal<CallStackElement>();

	public static void start() {
		CallStackElement cse = CallStackElement.newInstance(methodCallParent.get());
		methodCallParent.set(cse);
	}

	public static void stop(String signature) {
		CallStackElement currentStats = methodCallParent.get();
		currentStats.profile(signature, System.nanoTime() - currentStats.getExecutionTime());
		methodCallParent.set(currentStats.getParent());
	}

	public static void setMethodCallRoot(CallStackElement root) {
		methodCallParent.set(root);
	}

}
