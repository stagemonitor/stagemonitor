package de.isys.jawap.benchmark.aspectj;

import com.google.caliper.Benchmark;

public class InstrumentationBenchmark extends Benchmark {

	private InstrumentationTestObject instrumentationTestObject = new InstrumentationTestObject();

	public long timeNoInstrumentation(long reps) {
		Long dummy = new Long(0);
		for (Long i = new Long(0); i < reps; i++) {
			AspectJAspect.dummy++;
			dummy = instrumentationTestObject.testMethod(i);
			AspectJAspect.dummy++;
		}
		return dummy;
	}

	public long timeAround(long reps) {
		Long dummy = new Long(0);
		for (Long i = new Long(0); i < reps; i++) {
			dummy = instrumentationTestObject.instrumentationAroundTestMethod(i);
		}
		return dummy;
	}

	public long timeBeforeAfter(long reps) {
		Long dummy = new Long(0);
		for (Long i = new Long(0); i < reps; i++) {
			dummy = instrumentationTestObject.instrumentationBeforeAfterTestMethod(i);
		}
		return dummy;
	}

//	public static void main(String[] args) {
//			CaliperMain.main(InstrumentationBenchmark.class, args);
//		}

	public static void main(String[] args) {
		InstrumentationBenchmark instrumentationBenchmark = new InstrumentationBenchmark();
		final long warmups = 10000000L;
		final long runs = 100000000L;
		// warmup
		instrumentationBenchmark.timeNoInstrumentation(warmups);
		instrumentationBenchmark.timeAround(warmups);
		instrumentationBenchmark.timeBeforeAfter(warmups);

		// benchmark
		long start = System.nanoTime();
		instrumentationBenchmark.timeNoInstrumentation(runs);
		final long timeNoInstrumentation = System.nanoTime() - start;
		System.out.println("timeNoInstrumentation");
		System.out.println("total time: " + timeNoInstrumentation);
		System.out.println("time per method: " + (double) timeNoInstrumentation / runs);
		System.out.println("ratio: " + (double) timeNoInstrumentation / timeNoInstrumentation);

		start = System.nanoTime();
		instrumentationBenchmark.timeAround(runs);
		final long timeAround = System.nanoTime() - start;
		System.out.println("\ntimeAround");
		System.out.println("total time: " + timeAround);
		System.out.println("time per method: " + (double) timeAround / runs);
		System.out.println("ratio: " + (double) timeAround / timeNoInstrumentation);

		start = System.nanoTime();
		instrumentationBenchmark.timeBeforeAfter(runs);
		final long timeBeforeAfter = System.nanoTime() - start;
		System.out.println("\ntimeBeforeAfter");
		System.out.println("total time: " + timeBeforeAfter);
		System.out.println("time per method: " + (double) timeBeforeAfter / runs);
		System.out.println("ratio: " + (double) timeBeforeAfter / timeNoInstrumentation);


	}
}
