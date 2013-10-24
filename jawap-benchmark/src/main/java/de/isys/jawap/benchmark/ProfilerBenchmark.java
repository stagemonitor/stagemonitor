package de.isys.jawap.benchmark;

import de.isys.jawap.collector.model.HttpRequestContext;
import de.isys.jawap.collector.profile.Profiler;

public class ProfilerBenchmark {
	private static final int warmups = 10000000;
	private static final int runs = 10000000;
	private static final int noOfMethods = 7;

	private ClassToProfile classToProfile = new ClassToProfile();
	private ClassToProfile classToProfileDeactive = new ClassToProfile();
	private ClassNotToProfile classNotToProfile = new ClassNotToProfile();
	private ClassManualProfiling classManualProfiling = new ClassManualProfiling();

	public int testAspectJProfiler(int iter) {
		int dummy = 0;
		for (int i = 0; i < iter; i++) {
			Profiler.setExecutionContext(new HttpRequestContext());
			dummy |= classToProfile.method1();
		}
		return dummy;
	}

	public int testAspectJProfilerDeactivated(int iter) {
		if (Profiler.isProfilingActive()) throw new IllegalStateException("profiling is not deactivated!");
		int dummy = 0;
		for (int i = 0; i < iter; i++) {
			dummy |= classToProfileDeactive.method1();
		}
		return dummy;
	}

	public int testManualProfiling(int iter) {
		int dummy = 0;
		for (int i = 0; i < iter; i++) {
			dummy |= classManualProfiling.method1();
		}
		return dummy;
	}

	public int testNoProfiling(int iter) {
		int dummy = 0;
		for (int i = 0; i < iter; i++) {
			dummy |= classNotToProfile.method1();
		}
		return dummy;
	}

	public static void main(String[] args) {
		ProfilerBenchmark profilerBenchmark = new ProfilerBenchmark();

		System.out.println(profilerBenchmark.testNoProfiling(warmups));
		long start = System.nanoTime();
		System.out.println(profilerBenchmark.testNoProfiling(runs));
		long time = System.nanoTime() - start;
		System.out.println("no profiling:");
		System.out.println("total time: " + time);
		System.out.println("time per method: " + (double) time / runs / noOfMethods);

		System.out.println(profilerBenchmark.testAspectJProfilerDeactivated(warmups));
		long startAspectJDeactivated = System.nanoTime();
		System.out.println(profilerBenchmark.testAspectJProfilerDeactivated(runs));
		long timeAspectJDeactivated = System.nanoTime() - startAspectJDeactivated;
		System.out.println("profiling deactivated:");
		System.out.println("total time: " + timeAspectJDeactivated);
		System.out.println("time per method: " + (double) timeAspectJDeactivated / runs / noOfMethods);
		System.out.println("ratio: " + (double) timeAspectJDeactivated / time);

		System.out.println(profilerBenchmark.testAspectJProfiler(warmups));
		long startAspectJ = System.nanoTime();
		System.out.println(profilerBenchmark.testAspectJProfiler(runs));
		long timeAspectJ = System.nanoTime() - startAspectJ;
		System.out.println("profiling:");
		System.out.println("total time: " + timeAspectJ);
		System.out.println("time per method: " + (double) timeAspectJ / runs / noOfMethods);
		System.out.println("ratio: " + (double) timeAspectJ / time);

		System.out.println(profilerBenchmark.testManualProfiling(warmups));
		long startManual = System.nanoTime();
		System.out.println(profilerBenchmark.testManualProfiling(runs));
		long timeManual = System.nanoTime() - startManual;
		System.out.println("manual profiling:");
		System.out.println("total time: " + timeManual);
		System.out.println("time per method: " + (double) timeManual / runs / noOfMethods);
		System.out.println("ratio: " + (double) timeManual / time);

	}
}
