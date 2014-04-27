package org.stagemonitor.collector.profiler;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProfilerTest {
	private static class TestClass {
		static CallStackElement method1() {
			final CallStackElement callStackElement = Profiler.activateProfiling();
			try {
				method1_1();
				method1_2();
				return callStackElement;
			} finally {
				final CallStackElement thisCallStackElement = Profiler.getMethodCallParent();
				Profiler.stop("method1()");
				thisCallStackElement.setExecutionTime(10000000);
			}
		}

		static void method1_1() {
			Profiler.start();
			try {
				method1_1_1();
				method1_1_2();
			} finally {
				final CallStackElement thisCallStackElement = Profiler.getMethodCallParent();
				Profiler.stop("method1_1()");
				thisCallStackElement.setExecutionTime(5000000);

			}
		}

		static void method1_1_1() {
			Profiler.start();
			final CallStackElement thisCallStackElement = Profiler.getMethodCallParent();
			Profiler.stop("method1_1_1()");
			thisCallStackElement.setExecutionTime(2000000);

		}

		static void method1_1_2() {
			Profiler.start();
			final CallStackElement thisCallStackElement = Profiler.getMethodCallParent();
			Profiler.stop("method1_1_2()");
			thisCallStackElement.setExecutionTime(2500000);
		}

		static void method1_2() {
			Profiler.start();
			method1_2_1();
			final CallStackElement thisCallStackElement = Profiler.getMethodCallParent();
			Profiler.stop("method1_2()");
			thisCallStackElement.setExecutionTime(5000000);
		}
		static void method1_2_1() {
			Profiler.start();
			final CallStackElement thisCallStackElement = Profiler.getMethodCallParent();
			Profiler.stop("method1_2_1()");
			thisCallStackElement.setExecutionTime(2500000);
		}
	}

	@Test
	public void testProfiler() {
		final CallStackElement callStackElement = TestClass.method1();
		assertEquals(
						"000000,00  000%            000010,00  100% |||||||||| method1()\n" +
						"000000,50  005% :          000005,00  050% |||||      |-- method1_1()\n" +
						"000002,00  020% ||         000002,00  020% ||         |   |-- method1_1_1()\n" +
						"000002,50  025% ||:        000002,50  025% ||:        |   `-- method1_1_2()\n" +
						"000002,50  025% ||:        000005,00  050% |||||      `-- method1_2()\n" +
						"000002,50  025% ||:        000002,50  025% ||:            `-- method1_2_1()\n", callStackElement.toString());
	}

	@Test
	public void testProfilerActive() {
		assertFalse(Profiler.isProfilingActive());
		Profiler.activateProfiling();
		assertTrue(Profiler.isProfilingActive());
		Profiler.deactivateProfiling();
		assertFalse(Profiler.isProfilingActive());
	}

	@Test
	public void testNoProfilingIfNotActive() {
		assertFalse(Profiler.isProfilingActive());
		Profiler.start();
		assertNull(Profiler.getMethodCallParent());
		Profiler.stop("dummy");
	}

}
