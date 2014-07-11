package org.stagemonitor.benchmark.profiler;

import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.VmOptions;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;

import java.lang.management.ManagementFactory;

@VmOptions({"-Xmx6144m", "-Xms6144m", "-XX:NewSize=6000m", "-XX:MaxNewSize=6000m"})
public class VariableMethodsBenchmark {

	private ClassManualProfiling classManualProfiling = new ClassManualProfiling();

	@Param({"1", "10", "100", "1000"})
	private int iterations;

	@Benchmark
	public int testManualProfiling(int iter) {
		int dummy = 0;
		int innerIterations = iterations;
		CallStackElement root = null;
		for (int i = 0; i < iter; i++) {
			root = Profiler.activateProfiling("root");
			for (int j = 0; j < innerIterations; j++) {
				dummy |= classManualProfiling.method1();
			}
			Profiler.stop();
		}
		assertProfilingWorks(root);
		return dummy;
	}

	private void assertProfilingWorks(CallStackElement cse) {
		if (cse == null || cse.getChildren().size() != iterations) {
			System.out.println(cse);
			throw new IllegalStateException("profiling did not work! "+ ManagementFactory.getRuntimeMXBean().getInputArguments());
		}
	}

}
