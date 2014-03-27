package org.stagemonitor.benchmark.aspectj;

import com.google.caliper.Benchmark;
import com.google.caliper.api.VmOptions;

@VmOptions({"-Xmx6144m", "-Xms6144m", "-XX:NewSize=6000m", "-XX:MaxNewSize=6000m"})
public class InstrumentationBenchmark {

	private InstrumentationTestObject instrumentationTestObject = new InstrumentationTestObject();

	@Benchmark
	public long noAspectPrimitiveLong(long reps) {
		AspectJAspect.dummy = 0;
		long dummy = 0;
		for (long i = 0; i < reps; i++) {
			dummy = instrumentationTestObject.noAspectPrimitiveLong(i);
		}
		if (AspectJAspect.dummy != reps * 2)
			throw new IllegalStateException("Expected " + reps * 2 + " runs, actual runs: " + AspectJAspect.dummy);
		return dummy;
	}

	@Benchmark
	public long noAspectObjectLong(long reps) {
		AspectJAspect.dummy = 0;
		long dummy = 0;
		for (long i = 0; i < reps; i++) {
			dummy = instrumentationTestObject.noAspectObjectLong(i);
		}
		if (AspectJAspect.dummy != reps * 2)
			throw new IllegalStateException("Expected " + reps * 2 + " runs, actual runs: " + AspectJAspect.dummy);
		return dummy;
	}

	@Benchmark
	public long aroundPrimitiveLong(long reps) {
		AspectJAspect.dummy = 0;
		long dummy = 0;
		for (long i = 0; i < reps; i++) {
			dummy = instrumentationTestObject.aroundPrimitiveLong(i);
		}
		if (AspectJAspect.dummy != reps * 2)
			throw new IllegalStateException("Expected " + reps * 2 + " runs, actual runs: " + AspectJAspect.dummy);
		return dummy;
	}

	@Benchmark
	public long aroundObjectLong(long reps) {
		AspectJAspect.dummy = 0;
		long dummy = 0;
		for (long i = 0; i < reps; i++) {
			dummy = instrumentationTestObject.aroundObjectLong(i);
		}
		if (AspectJAspect.dummy != reps * 2)
			throw new IllegalStateException("Expected " + reps * 2 + " runs, actual runs: " + AspectJAspect.dummy);
		return dummy;
	}

	@Benchmark
	public long beforeAfterPrimitiveLong(long reps) {
		AspectJAspect.dummy = 0;
		long dummy = 0;
		for (long i = 0; i < reps; i++) {
			dummy = instrumentationTestObject.beforeAfterPrimitiveLong(i);
		}
		if (AspectJAspect.dummy != reps * 2)
			throw new IllegalStateException("Expected " + reps * 2 + " runs, actual runs: " + AspectJAspect.dummy);
		return dummy;
	}

	@Benchmark
	public long beforeAfterObjectLong(long reps) {
		AspectJAspect.dummy = 0;
		long dummy = 0;
		for (long i = 0; i < reps; i++) {
			dummy = instrumentationTestObject.beforeAfterObjectLong(i);
		}
		if (AspectJAspect.dummy != reps * 2)
			throw new IllegalStateException("Expected " + reps * 2 + " runs, actual runs: " + AspectJAspect.dummy);
		return dummy;
	}
}
