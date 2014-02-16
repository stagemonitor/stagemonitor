package de.isys.jawap.benchmark.profiler;

import com.google.caliper.api.VmOptions;
import de.isys.jawap.entities.web.HttpExecutionContext;
import de.isys.jawap.collector.profiler.Profiler;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;
import java.util.List;

@VmOptions({"-Xmx6144m", "-Xms6144m", "-XX:NewSize=6000m", "-XX:MaxNewSize=6000m"})
public class ProfilerBenchmark {
	private static final int warmups = 2;
	private static final int runs    = 10000000;
	private static final int noOfMethods = 7;

	private ClassToProfile classToProfile = new ClassToProfile();
	private ClassToProfile classToProfileDeactive = new ClassToProfile();
	private ClassNotToProfile classNotToProfile = new ClassNotToProfile();
	private ClassJavassistProfiled classJavassistProfiled = new ClassJavassistProfiled();
	private ClassManualProfiling classManualProfiling = new ClassManualProfiling();
	private ClassOptimalPerformanceProfied classOptimalPerformanceProfied = new ClassOptimalPerformanceProfied();

	@com.google.caliper.Benchmark
	public int testNoProfiling(long iter) {
		int dummy = 0;
		for (int i = 0; i < iter; i++) {
			dummy |= classNotToProfile.method1();
		}
		return dummy;
	}

//	@com.google.caliper.Benchmark
	public int testNoProfilingSettingExecutionContext(long iter) {
		int dummy = 0;
		HttpExecutionContext executionContext = new HttpExecutionContext();
		for (int i = 0; i < iter; i++) {
			Profiler.setExecutionContext(executionContext);
			executionContext.setCallStack(null);
			dummy |= classNotToProfile.method1();
		}
		return dummy;
	}

	@com.google.caliper.Benchmark
	public int testOptimalPerformanceProfiling(int iter) {
		int dummy = 0;
		for (int i = 0; i < iter; i++) {
			OptimalPerformanceProfilerMock.times.clear();
			OptimalPerformanceProfilerMock.signatures.clear();
			dummy |= classOptimalPerformanceProfied.method1();
		}
		if (OptimalPerformanceProfilerMock.times.isEmpty() || OptimalPerformanceProfilerMock.signatures.isEmpty()) {
			throw new IllegalArgumentException("Profiling did not work");
		}
		return dummy;
	}

	@com.google.caliper.Benchmark
	public int testManualProfiling(int iter) {
		int dummy = 0;
		HttpExecutionContext executionContext = new HttpExecutionContext();
		for (int i = 0; i < iter; i++) {
			Profiler.setExecutionContext(executionContext);
			executionContext.setCallStack(null);
			dummy |= classManualProfiling.method1();
		}
		assertProfilingWorks(executionContext);
		return dummy;
	}

	@com.google.caliper.Benchmark
	public int testJavassistProfilingDeactivated(int iter) {
		Profiler.setExecutionContext(null);
		if (Profiler.isProfilingActive()) throw new IllegalStateException("profiling is not deactivated!");
		int dummy = 0;
		for (int i = 0; i < iter; i++) {
			dummy |= classJavassistProfiled.method1();
		}
		return dummy;
	}
	@com.google.caliper.Benchmark
	public int testJavassistProfiling(int iter) {
		int dummy = 0;
		HttpExecutionContext executionContext = new HttpExecutionContext();
		for (int i = 0; i < iter; i++) {
			Profiler.setExecutionContext(executionContext);
			executionContext.setCallStack(null);
			dummy |= classJavassistProfiled.method1();
		}
		boolean dryRun = iter == 1;
		if (!dryRun) {
			assertProfilingWorks(executionContext);
		}
		return dummy;
	}

	@com.google.caliper.Benchmark
	public int testAspectJProfilerDeactivated(int iter) {
		Profiler.setExecutionContext(null);
		if (Profiler.isProfilingActive()) throw new IllegalStateException("profiling is not deactivated!");
		int dummy = 0;
		for (int i = 0; i < iter; i++) {
			dummy |= classToProfileDeactive.method1();
		}
		return dummy;
	}

	@com.google.caliper.Benchmark
	public int testAspectJProfiler(int iter) {
		int dummy = 0;
		HttpExecutionContext executionContext = new HttpExecutionContext();
		for (int i = 0; i < iter; i++) {
			Profiler.setExecutionContext(executionContext);
			executionContext.setCallStack(null);
			dummy |= classToProfile.method1();
		}
		assertProfilingWorks(executionContext);
		return dummy;
	}

	private void assertProfilingWorks(HttpExecutionContext executionContext) {
		if (executionContext.getCallStack() == null ||!executionContext.getCallStack().getSignature().contains("method1")) {
			System.out.println(executionContext);
			throw new IllegalStateException("profiling did not work! "+ ManagementFactory.getRuntimeMXBean().getInputArguments());
		}
	}

	public static void main(String[] args) {
		System.out.println("Total memory (MB): " + Runtime.getRuntime().totalMemory() / 1024 / 1024);
		System.out.println("Max   memory (MB): " + Runtime.getRuntime().maxMemory() / 1024 / 1024);

		final ProfilerBenchmark profilerBenchmark = new ProfilerBenchmark();
		System.out.println("_____________________\n");

		long timeNoProfiling = performBenchmark("#no profiling", null, args, new Benchmark() {
			@Override
			public int performBenchmark(int runs) {
				return profilerBenchmark.testNoProfiling(runs);
			}
		});
		performBenchmark("#no profiling settingExecutionContext", timeNoProfiling, args, new Benchmark() {
			@Override
			public int performBenchmark(int runs) {
				return profilerBenchmark.testNoProfilingSettingExecutionContext(runs);
			}
		});
		performBenchmark("#optimal", timeNoProfiling,args, new Benchmark() {
			@Override
			public int performBenchmark(int runs) {
				return profilerBenchmark.testOptimalPerformanceProfiling(runs);
			}
		});
		performBenchmark("#manual", timeNoProfiling,args, new Benchmark() {
			@Override
			public int performBenchmark(int runs) {
				return profilerBenchmark.testManualProfiling(runs);
			}
		});
		performBenchmark("#javassist deactivated", timeNoProfiling,args, new Benchmark() {
			@Override
			public int performBenchmark(int runs) {
				return profilerBenchmark.testJavassistProfilingDeactivated(runs);
			}
		});
		performBenchmark("#javassist", timeNoProfiling,args, new Benchmark() {
			@Override
			public int performBenchmark(int runs) {
				return profilerBenchmark.testJavassistProfiling(runs);
			}
		});
		performBenchmark("#aspectj deactivated", timeNoProfiling,args, new Benchmark() {
			@Override
			public int performBenchmark(int runs) {
				return profilerBenchmark.testAspectJProfilerDeactivated(runs);
			}
		});
		performBenchmark("#aspectj", timeNoProfiling,args, new Benchmark() {
			@Override
			public int performBenchmark(int runs) {
				return profilerBenchmark.testAspectJProfiler(runs);
			}
		});

	}

	private static long performBenchmark(String title, Long timeNoProfiling, String[] args, Benchmark benchmark) {
		String abbr = "";
		for (String s : title.replace("#", "").split(" ")) {
			abbr += s.substring(0,1);
		}

		final List<String> argList = Arrays.asList(args);
		if (argList.isEmpty() || argList.contains(abbr)) {
			int dummy = 0;
			for (int i = 0; i < warmups; i++) {
				gc();
				System.out.println("warmup " + i + " runs: " + runs);
				dummy |= benchmark.performBenchmark(runs);
			}		
			
			gc();
			System.out.println("measuring runs: " + runs);
			long start = System.nanoTime();
			dummy |= benchmark.performBenchmark(runs);
			long time = System.nanoTime() - start;
			System.out.println("measuring done");
			if (timeNoProfiling == null) {
				timeNoProfiling = time;
			}
			reportTime(title, time, timeNoProfiling);

			System.out.println(dummy + "_____________________\n");
			return timeNoProfiling;
		}
		return -1;
	}

	interface Benchmark {
		int performBenchmark(int runs);
	}


	private static void reportTime(String title, long time,long timeNoProfiling) {
		final double timePerMethod = (double) time / runs / noOfMethods;
		System.out.println(title+","+timePerMethod);
		System.out.println("total time: " + time);
		System.out.println("time per method: " + timePerMethod);
		//System.out.println("overhead/method: " + (double) (time - timeNoProfiling) / runs / noOfMethods);
	}

	private static void gc() {
		System.gc();
		System.gc();
		System.gc();
		System.runFinalization();
		System.runFinalization();
		System.runFinalization();
	}
}
