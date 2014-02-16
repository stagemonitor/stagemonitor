package de.isys.jawap.benchmark.profiler;

import com.google.caliper.Param;
import com.google.caliper.api.VmOptions;
import de.isys.jawap.collector.profiler.Profiler;
import de.isys.jawap.entities.web.HttpExecutionContext;

import java.lang.management.ManagementFactory;

@VmOptions({"-Xmx6144m", "-Xms6144m", "-XX:NewSize=6000m", "-XX:MaxNewSize=6000m"})
public class VariableMethodsBenchmark {

	private ClassManualProfiling classManualProfiling = new ClassManualProfiling();

	@Param({"1", "10", "100", "1000"})
	private int iterations;

	@com.google.caliper.Benchmark
	public int testManualProfiling(int iter) {
		int dummy = 0;
		int innerIterations = iterations;
		HttpExecutionContext executionContext = new HttpExecutionContext();
		for (int i = 0; i < iter; i++) {
			Profiler.setExecutionContext(executionContext);
			executionContext.setCallStack(null);
			Profiler.start();
			for (int j = 0; j < innerIterations; j++) {
				dummy |= classManualProfiling.method1();
			}
			Profiler.stop("root");
		}
		assertProfilingWorks(executionContext);
		return dummy;
	}

	private void assertProfilingWorks(HttpExecutionContext executionContext) {
		if (executionContext.getCallStack() == null ||executionContext.getCallStack().getChildren().size() != iterations) {
			System.out.println(executionContext);
			throw new IllegalStateException("profiling did not work! "+ ManagementFactory.getRuntimeMXBean().getInputArguments());
		}
	}

}
