package org.stagemonitor.benchmark.profiler;

import java.lang.management.ManagementFactory;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;

@State(value = Scope.Benchmark)
public class ProfilerBenchmark {

	private ClassNotToProfile classNotToProfile;
	private ClassJavassistProfiled classJavassistProfiled;
	private ClassManualProfiling classManualProfiling;
	private ClassOptimalPerformanceProfied classOptimalPerformanceProfied;

	@Setup
	public void init() {
		classNotToProfile = new ClassNotToProfile();
		classJavassistProfiled = new ClassJavassistProfiled();
		classManualProfiling = new ClassManualProfiling();
		classOptimalPerformanceProfied = new ClassOptimalPerformanceProfied();

		Profiler.deactivateProfiling();
		assertProfilingWorks(manual());
		assertProfilingWorks(javassist());
		Profiler.deactivateProfiling();
	}

	@Benchmark
	public int noProfiling() {
		return classNotToProfile.method1();
	}

	@Benchmark
	public int theoreticalOptimum() {
		OptimalPerformanceProfilerMock.clear();
		OptimalPerformanceProfilerMock.start();
		try {
			return classOptimalPerformanceProfied.method1();
		} finally {
			OptimalPerformanceProfilerMock.stop("root");
		}
	}

	@Benchmark
	public CallStackElement manual() {
		CallStackElement root = Profiler.activateProfiling("root");
		classManualProfiling.method1();
		Profiler.stop();
		return root;
	}

	@Benchmark
	public int javassistDeactivated() {
		return classJavassistProfiled.method1();
	}

	@Benchmark
	public CallStackElement javassist() {
		CallStackElement root = Profiler.activateProfiling("root");
		classJavassistProfiled.method1();
		Profiler.stop();
		return root;
	}

	private static void assertProfilingWorks(CallStackElement cse) {
		if (cse.getChildren().isEmpty() || !cse.getChildren().get(0).getSignature().contains("method1")) {
			throw new IllegalStateException("profiling did not work! " +
					ManagementFactory.getRuntimeMXBean().getInputArguments() +  "\n" + cse);
		}
	}
}
