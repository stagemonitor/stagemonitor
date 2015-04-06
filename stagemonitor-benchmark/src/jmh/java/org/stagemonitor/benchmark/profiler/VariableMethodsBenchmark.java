package org.stagemonitor.benchmark.profiler;

import java.lang.management.ManagementFactory;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;

@Fork(jvmArgs = {"-Xmx6144m", "-Xms6144m", "-XX:NewSize=6000m", "-XX:MaxNewSize=6000m"})
@State(Scope.Benchmark)
public class VariableMethodsBenchmark {

	private ClassManualProfiling classManualProfiling = new ClassManualProfiling();

	@Setup
	public void setUp() {
		assertProfilingWorks(testManualProfiling());
	}

	@Param({"1", "10", "100", "1000"})
	private int iterations;

	@Benchmark
	public CallStackElement testManualProfiling() {
		int innerIterations = iterations;
		CallStackElement root = Profiler.activateProfiling("root");
		for (int i = 0; i < innerIterations; i++) {
			classManualProfiling.method1();
		}
		Profiler.stop();
		assertProfilingWorks(root);
		return root;
	}

	private void assertProfilingWorks(CallStackElement cse) {
		if (cse == null || cse.getChildren().size() != iterations) {
			System.out.println(cse);
			throw new IllegalStateException("profiling did not work! " + ManagementFactory.getRuntimeMXBean().getInputArguments());
		}
	}

}
