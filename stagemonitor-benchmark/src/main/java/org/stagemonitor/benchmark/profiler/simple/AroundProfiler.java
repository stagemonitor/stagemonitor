package org.stagemonitor.benchmark.profiler.simple;

import org.stagemonitor.requestmonitor.profiler.CallStackElement;

public class AroundProfiler {
  static final ThreadLocal<CallStackElement> methodCallParent =
      new ThreadLocal<CallStackElement>();

  public static CallStackElement start(String signature) {
    CallStackElement cse = new CallStackElement(methodCallParent.get(), signature);
    methodCallParent.set(cse);
    return cse;
  }

  public static void stop(CallStackElement currentStats) {
    currentStats.executionStopped(System.nanoTime() - currentStats.getExecutionTime());
    methodCallParent.set(currentStats.getParent());
  }

  public static void setMethodCallRoot(CallStackElement root) {
    methodCallParent.set(root);
  }
}
