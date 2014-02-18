package de.isys.jawap.benchmark.profiler.simple;

import de.isys.jawap.entities.profiler.CallStackElement;

public class AroundProfiler {
  static final ThreadLocal<CallStackElement> methodCallParent =
      new ThreadLocal<CallStackElement>();

  public static CallStackElement start() {
    CallStackElement cse = CallStackElement.
        newInstance(methodCallParent.get());
    methodCallParent.set(cse);
    return cse;
  }

  public static void stop(CallStackElement currentStats,
                          String signature) {
    currentStats.executionStopped(signature, System.nanoTime() -
			currentStats.getExecutionTime());
    methodCallParent.set(currentStats.getParent());
  }

  public static void setMethodCallRoot(CallStackElement root) {
    methodCallParent.set(root);
  }
}
