package org.stagemonitor.benchmark.profiler;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.profiler.CallStackElement;
import org.stagemonitor.tracing.profiler.Profiler;

//@Fork(jvmArgs = {"-Xmx6144m", "-Xms6144m", "-XX:NewSize=6000m", "-XX:MaxNewSize=6000m"})
@State(Scope.Benchmark)
public class VariableMethodsBenchmark {

	private ClassManualProfiling classManualProfiling = new ClassManualProfiling();

	public static void main(String[] args) {
		final VariableMethodsBenchmark benchmark = new VariableMethodsBenchmark();
		long start = System.currentTimeMillis();
		long dummy = 0;
		for (int i = 0; i < 10_000; i++) {
			dummy |= benchmark.testManualProfiling().getExecutionTime();
		}
		System.out.println(dummy);
		System.out.println(System.currentTimeMillis() - start);
//		System.out.println("Size of objectPool: " + CallStackElement.objectPool.size());
	}

	@Setup
	public void init() {
		System.out.println("object pooling: " + Stagemonitor.getPlugin(TracingPlugin.class).isProfilerObjectPoolingActive());
	}

	@Param({"1000"})
	private int iterations = 1000;

	@Benchmark
	public CallStackElement testManualProfiling() {
		int innerIterations = iterations;
		CallStackElement root = Profiler.activateProfiling("root");
		for (int i = 0; i < innerIterations; i++) {
			classManualProfiling.method1();
		}
		Profiler.stop();
		root.recycle();
		return root;
	}

}
