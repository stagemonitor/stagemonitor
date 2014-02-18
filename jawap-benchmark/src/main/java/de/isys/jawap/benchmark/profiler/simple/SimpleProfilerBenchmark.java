package de.isys.jawap.benchmark.profiler.simple;

import com.google.caliper.Benchmark;
import com.google.caliper.api.VmOptions;
import de.isys.jawap.entities.profiler.CallStackElement;

@VmOptions({"-Xmx6144m", "-Xms6144m", "-XX:NewSize=6000m", "-XX:MaxNewSize=6000m" })
public class SimpleProfilerBenchmark {

	private AroundProfilerTest aroundProfilerTest = new AroundProfilerTest();
	private BeforeAfterProfilerTest beforeAfterProfilerTest = new BeforeAfterProfilerTest();

//	@Benchmark
	public int aroundProfilerTest(int iter) {
		int dummy = 0;
		CallStackElement root = null;
		for (int i = 0; i < iter; i++) {
			root = new CallStackElement();
			AroundProfiler.setMethodCallRoot(root);
			dummy |= aroundProfilerTest.method1();
			AroundProfiler.stop(root, "root");
		}
		if (!root.getChildren().get(0).getSignature().contains("method1")) {
			throw new IllegalStateException("profiling did not work");
		}
		return dummy;
	}

	@Benchmark
	public int beforeAfterProfiler(int iter) {
		int dummy = 0;
		CallStackElement root = null;
		for (int i = 0; i < iter; i++) {
			root = new CallStackElement();
			BeforeAfterProfiler.setMethodCallRoot(root);
			dummy |= beforeAfterProfilerTest.method1();
			BeforeAfterProfiler.stop("root");
		}
		if (!root.getChildren().get(0).getSignature().contains("method1")) {
			throw new IllegalStateException("profiling did not work");
		}
		return dummy;
	}
}
