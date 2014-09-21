package org.stagemonitor.requestmonitor.profiler;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProfilerTest {
	private static class TestClass {
		static CallStackElement method1() {
			final CallStackElement callStackElement = Profiler.activateProfiling("method1()");
			try {
				method1_1();
				method1_2();
				return callStackElement;
			} finally {
				final CallStackElement thisCallStackElement = Profiler.getMethodCallParent();
				Profiler.stop();
				thisCallStackElement.setExecutionTime(1000000000);
			}
		}

		static void method1_1() {
			Profiler.start("method1_1()");
			try {
				method1_1_1();
				method1_1_2();
			} finally {
				final CallStackElement thisCallStackElement = Profiler.getMethodCallParent();
				Profiler.stop();
				thisCallStackElement.setExecutionTime(500000000);

			}
		}

		static void method1_1_1() {
			Profiler.start("method1_1_1()");
			final CallStackElement thisCallStackElement = Profiler.getMethodCallParent();
			Profiler.stop();
			thisCallStackElement.setExecutionTime(200000000);

		}

		static void method1_1_2() {
			Profiler.start("method1_1_2()");
			try {
				method1_1_2_1();
			} finally {
				final CallStackElement thisCallStackElement = Profiler.getMethodCallParent();
				Profiler.stop();
				thisCallStackElement.setExecutionTime(250000000);
			}
		}

		static void method1_1_2_1() {
			Profiler.start("method1_1_2_1()");
			final CallStackElement thisCallStackElement = Profiler.getMethodCallParent();
			Profiler.stop();
			thisCallStackElement.setExecutionTime(50000000);
		}

		static void method1_2() {
			Profiler.start("method1_2()");
			Profiler.addCall("select * from user", 50000000);
			Profiler.addCall("select * from address", 50000000);
			method1_2_1();
			final CallStackElement thisCallStackElement = Profiler.getMethodCallParent();
			Profiler.stop();
			thisCallStackElement.setExecutionTime(500000000);
		}
		static void method1_2_1() {
			Profiler.start("method1_2_1()");
			final CallStackElement thisCallStackElement = Profiler.getMethodCallParent();
			Profiler.stop();
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
						"000200.00  020% ||-------- 000250.00  025% ||:------- |   `-- method1_1_2()\n" +
						"000050.00  005% :--------- 000050.00  005% :--------- |       `-- method1_1_2_1()\n" +
						"000150.00  015% |:-------- 000500.00  050% |||||----- `-- method1_2()\n" +
						"000050.00  005% :--------- 000050.00  005% :---------     |-- select * from user\n" +
						"000050.00  005% :--------- 000050.00  005% :---------     |-- select * from address\n" +
						"000250.00  025% ||:------- 000250.00  025% ||:-------     `-- method1_2_1()\n", callStackElement.toString());
	}

	@Test
	public void testProfilerActive() {
		assertFalse(Profiler.isProfilingActive());
		Profiler.activateProfiling("");
		assertTrue(Profiler.isProfilingActive());
		Profiler.deactivateProfiling();
		assertFalse(Profiler.isProfilingActive());
	}

	@Test
	public void testNoProfilingIfNotActive() {
		assertFalse(Profiler.isProfilingActive());
		Profiler.start("dummy");
		assertNull(Profiler.getMethodCallParent());
		Profiler.stop();
	}

}
