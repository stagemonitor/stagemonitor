package org.stagemonitor.benchmark.profiler;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.tracing.profiler.CallStackElement;
import org.stagemonitor.tracing.profiler.Profiler;

import java.lang.management.ManagementFactory;

@State(value = Scope.Benchmark)
public class ProfilerBenchmark {

	private ClassNotToProfile classNotToProfile;
	private ClassJavassistProfiled classJavassistProfiled;
//	private ClassByteBuddyProfiled classByteBuddyProfiled;
	private ClassManualProfiling classManualProfiling;
	private ClassOptimalPerformanceProfied classOptimalPerformanceProfied;

	public static void main(String[] args) {
		new ProfilerBenchmark().init();
	}

	@Setup
	public void init() {
		Stagemonitor.init();
		classNotToProfile = new ClassNotToProfile();
		classJavassistProfiled = new ClassJavassistProfiled();
		classManualProfiling = new ClassManualProfiling();
		classOptimalPerformanceProfied = new ClassOptimalPerformanceProfied();
//		classByteBuddyProfiled = new ClassByteBuddyProfiled();

		Profiler.deactivateProfiling();
		assertProfilingWorks(manual());
//		assertProfilingWorks(byteBuddy());
//		assertProfilingWorks(javassist());
		Profiler.deactivateProfiling();
	}

	//@Benchmark
	public int noProfiling() {
		return classNotToProfile.method1();
	}

	//@Benchmark
	public int theoreticalOptimum() {
		OptimalPerformanceProfilerMock.clear();
		OptimalPerformanceProfilerMock.start();
		try {
			return classOptimalPerformanceProfied.method1();
		} finally {
			OptimalPerformanceProfilerMock.stop("root");
		}
	}

	//@Benchmark
	public CallStackElement manual() {
		CallStackElement root = Profiler.activateProfiling("root");
		classManualProfiling.method1();
		Profiler.stop();
		return root;
	}

//	//@Benchmark
	public int javassistDeactivated() {
		return classJavassistProfiled.method1();
	}

//	//@Benchmark
	public CallStackElement javassist() {
		CallStackElement root = Profiler.activateProfiling("root");
		classJavassistProfiled.method1();
		Profiler.stop();
		return root;
	}
	
//	//@Benchmark
//	public CallStackElement byteBuddy() {
//		CallStackElement root = Profiler.activateProfiling("root");
//		classByteBuddyProfiled.method1();
//		Profiler.stop();
//		return root;
//	}

	private static void assertProfilingWorks(CallStackElement cse) {
		if (cse.getChildren().isEmpty() || !cse.getChildren().get(0).getSignature().contains("method1")) {
			throw new IllegalStateException("profiling did not work! " +
					ManagementFactory.getRuntimeMXBean().getInputArguments() +  "\n" + cse);
		}
	}
}
