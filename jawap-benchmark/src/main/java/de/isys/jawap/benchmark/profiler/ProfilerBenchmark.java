package de.isys.jawap.benchmark.profiler;

import com.google.caliper.Benchmark;
import com.google.caliper.api.VmOptions;
import de.isys.jawap.entities.profiler.CallStackElement;
import de.isys.jawap.collector.profiler.Profiler;

import java.lang.management.ManagementFactory;

@VmOptions({"-Xmx6144m", "-Xms6144m", "-XX:NewSize=6000m", "-XX:MaxNewSize=6000m"})
public class ProfilerBenchmark {

	private ClassToProfile classToProfile = new ClassToProfile();
	private ClassToProfile classToProfileDeactive = new ClassToProfile();
	private ClassNotToProfile classNotToProfile = new ClassNotToProfile();
	private ClassJavassistProfiled classJavassistProfiled = new ClassJavassistProfiled();
	private ClassManualProfiling classManualProfiling = new ClassManualProfiling();
	private ClassOptimalPerformanceProfied classOptimalPerformanceProfied = new ClassOptimalPerformanceProfied();

//	@Benchmark
	public int noProfiling(long iter) {
		int dummy = 0;
		for (int i = 0; i < iter; i++) {
			dummy |= classNotToProfile.method1();
		}
		return dummy;
	}

	@Benchmark
	public int theoreticalOptimum(int iter) {
		int dummy = 0;
		for (int i = 0; i < iter; i++) {
			OptimalPerformanceProfilerMock.times.clear();
			OptimalPerformanceProfilerMock.signatures.clear();
			OptimalPerformanceProfilerMock.start();
			dummy |= classOptimalPerformanceProfied.method1();
			OptimalPerformanceProfilerMock.stop("root");
		}
		if (OptimalPerformanceProfilerMock.times.isEmpty() || OptimalPerformanceProfilerMock.signatures.isEmpty()) {
			throw new IllegalArgumentException("Profiling did not work");
		}
		return dummy;
	}

	@Benchmark
	public int manual(int iter) {
		int dummy = 0;
		CallStackElement root = null;
		for (int i = 0; i < iter; i++) {
			root = Profiler.activateProfiling();
			dummy |= classManualProfiling.method1();
			Profiler.stop("root");
		}
		assertProfilingWorks(root, iter);
		return dummy;
	}


	@Benchmark
	public int javassistDeactivated(int iter) {
		Profiler.deactivateProfiling();
		if (Profiler.isProfilingActive()) throw new IllegalStateException("profiling is not deactivated!");
		int dummy = 0;
		for (int i = 0; i < iter; i++) {
			dummy |= classJavassistProfiled.method1();
		}
		return dummy;
	}
	@Benchmark
	public int javassist(int iter) {
		int dummy = 0;
		CallStackElement root = null;
		for (int i = 0; i < iter; i++) {
			root = Profiler.activateProfiling();
			dummy |= classJavassistProfiled.method1();
			Profiler.stop("root");
		}
		assertProfilingWorks(root, iter);

		return dummy;
	}

	@Benchmark
	public int aspectJDeactivated(int iter) {
		Profiler.deactivateProfiling();
		if (Profiler.isProfilingActive()) throw new IllegalStateException("profiling is not deactivated!");
		int dummy = 0;
		for (int i = 0; i < iter; i++) {
			dummy |= classToProfileDeactive.method1();
		}
		return dummy;
	}

	@Benchmark
	public int aspectJ(int iter) {
		int dummy = 0;
		CallStackElement root = null;
		for (int i = 0; i < iter; i++) {
			root = Profiler.activateProfiling();
			dummy |= classToProfile.method1();
			Profiler.stop("root");
		}
		if (iter > 0) assertProfilingWorks(root, iter);
		return dummy;
	}

	private void assertProfilingWorks(CallStackElement cse, int iter) {
		boolean dryRun = iter <= 1;
		if (dryRun) return;
		if (cse.getChildren().isEmpty() || !cse.getChildren().get(0).getSignature().contains("method1")) {
			System.out.println(cse);
			throw new IllegalStateException("profiling did not work! "+ ManagementFactory.getRuntimeMXBean().getInputArguments());
		}
	}
}
