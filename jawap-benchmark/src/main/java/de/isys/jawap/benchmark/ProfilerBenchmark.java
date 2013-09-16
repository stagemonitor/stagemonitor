package de.isys.jawap.benchmark;

import de.isys.jawap.collector.model.HttpRequestStats;
import de.isys.jawap.collector.profile.Profiler;
import de.isys.jawap.collector.service.DefaultPerformanceMeasuringService;
import de.isys.jawap.collector.service.PerformanceMeasuringService;

public class ProfilerBenchmark {
	private static final int warmups = 10000000;
	private static final int runs = 100000000;
	private static final int noOfMethods = 7;

	private ClassToProfile classToProfile = new ClassToProfile();
	private PerformanceMeasuringService performanceMeasuringService = new DefaultPerformanceMeasuringService();

	public int testAspectJProfiler(int iter) {
		HttpRequestStats httpRequestStats = new HttpRequestStats();
		int dummy = 0;
		for (int i = 0; i < iter; i++) {
			Profiler.setCurrentRequestStats(httpRequestStats);
			dummy |= classToProfile.method1();
		}
		performanceMeasuringService.logStats(httpRequestStats);
		return dummy;
	}

	public int testManualProfiling(int iter) {
		HttpRequestStats httpRequestStats = new HttpRequestStats();
		int dummy = 0;
		for (int i = 0; i < iter; i++) {
			Profiler.setCurrentRequestStats(httpRequestStats);
			dummy |= classToProfile.method1();
		}
		performanceMeasuringService.logStats(httpRequestStats);
		return dummy;
	}

	public int testNoProfiling(int iter) {
		int dummy = 0;
		for (int i = 0; i < iter; i++) {
			dummy |= classToProfile.method1();
		}
		return dummy;
	}

	public static void main(String[] args) {
		ProfilerBenchmark profilerBenchmark = new ProfilerBenchmark();
		// warmup
		System.out.println(profilerBenchmark.testAspectJProfiler(warmups));
		System.out.println(profilerBenchmark.testNoProfiling(warmups));
		System.out.println(profilerBenchmark.testManualProfiling(warmups));

		// benchmark
		long start = System.nanoTime();
		System.out.println(profilerBenchmark.testNoProfiling(warmups));
		long time = System.nanoTime() - start;
		System.out.println("no profiling:");
		System.out.println("total time: " + time);
		System.out.println("time per method: " + (double) time / runs / noOfMethods);

		long startAspectJ = System.nanoTime();
		System.out.println(profilerBenchmark.testAspectJProfiler(warmups));
		long timeAspectJ = System.nanoTime() - startAspectJ;
		System.out.println("profiling:");
		System.out.println("total time: " + timeAspectJ);
		System.out.println("time per method: " + (double) timeAspectJ / runs / noOfMethods);
		System.out.println("ratio: " + (double) timeAspectJ / time);

		long startManual = System.nanoTime();
		System.out.println(profilerBenchmark.testManualProfiling(warmups));
		long timeManual = System.nanoTime() - startManual;
		System.out.println("manual profiling:");
		System.out.println("total time: " + timeManual);
		System.out.println("time per method: " + (double) timeManual / runs / noOfMethods);
		System.out.println("ratio: " + (double) timeManual / time);

	}
}
