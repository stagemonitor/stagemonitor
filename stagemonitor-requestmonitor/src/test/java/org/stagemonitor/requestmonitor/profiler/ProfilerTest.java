package org.stagemonitor.requestmonitor.profiler;

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
				thisCallStackElement.setExecutionTime(1000000000);
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
				thisCallStackElement.setExecutionTime(500000000);

			}
		}

		static void method1_1_1() {
			Profiler.start();
			final CallStackElement thisCallStackElement = Profiler.getMethodCallParent();
			Profiler.stop("method1_1_1()");
			thisCallStackElement.setExecutionTime(200000000);

		}

		static void method1_1_2() {
			Profiler.start();
			final CallStackElement thisCallStackElement = Profiler.getMethodCallParent();
			Profiler.stop("method1_1_2()");
			thisCallStackElement.setExecutionTime(250000000);
		}

		static void method1_2() {
			Profiler.start();
			method1_2_1();
			final CallStackElement thisCallStackElement = Profiler.getMethodCallParent();
			Profiler.stop("method1_2()");
			thisCallStackElement.setExecutionTime(500000000);
		}
		static void method1_2_1() {
			Profiler.start();
			final CallStackElement thisCallStackElement = Profiler.getMethodCallParent();
			Profiler.stop("method1_2_1()");
			thisCallStackElement.setExecutionTime(250000000);
		}
	}

	@Test
	public void testProfiler() {
		final CallStackElement callStackElement = TestClass.method1();
		assertEquals(
						"----------------------------------------------------------------------\n" +
						"Selftime (ms)              Total (ms)                 Method signature\n" +
						"----------------------------------------------------------------------\n" +
						"000000.00  000% ---------- 001000.00  100% |||||||||| method1()\n" +
						"000050.00  005% :--------- 000500.00  050% |||||----- |-- method1_1()\n" +
						"000200.00  020% ||-------- 000200.00  020% ||-------- |   |-- method1_1_1()\n" +
						"000250.00  025% ||:------- 000250.00  025% ||:------- |   `-- method1_1_2()\n" +
						"000250.00  025% ||:------- 000500.00  050% |||||----- `-- method1_2()\n" +
						"000250.00  025% ||:------- 000250.00  025% ||:-------     `-- method1_2_1()\n", callStackElement.toString());
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
