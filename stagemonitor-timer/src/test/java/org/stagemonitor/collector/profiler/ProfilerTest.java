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
		assertEquals(
				"     0,00    0%                10,00  100% |||||||||| method1()\n" +
				"     0,50    5% -               5,00   50% |||||      |-- method1_1()\n" +
				"     2,00   20% ||              2,00   20% ||         |   |-- method1_1_1()\n" +
				"     2,50   25% ||-             2,50   25% ||-        |   `-- method1_1_2()\n" +
				"     2,50   25% ||-             5,00   50% |||||      `-- method1_2()\n" +
				"     2,50   25% ||-             2,50   25% ||-            `-- method1_2_1()\n", TestClass.method1().toString());
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
