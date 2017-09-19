package org.stagemonitor.tracing.profiler;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ManualProfilerTest {
	private static class TestClass {
		static CallStackElement method0() {
			final CallStackElement callStackElement = Profiler.activateProfiling("method0()");
			try {
				method1();
				return callStackElement;
			} finally {
				final CallStackElement thisCallStackElement = Profiler.getMethodCallParent();
				Profiler.stop();
				thisCallStackElement.setExecutionTime(1000000000);
			}
		}

		static void method1() {
			Profiler.start("method1()");
			try {
				method1_1();
				method1_2();
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
			Profiler.addIOCall("select * from user", 50000000);
			Profiler.addIOCall("select * from address", 50000000);
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
		final CallStackElement callStackElement = TestClass.method0();
		assertEquals(
						"----------------------------------------------------------------------\n" +
						"Selftime (ms)              Total (ms)                 Method signature\n" +
						"----------------------------------------------------------------------\n" +
						"000000.00  000% ---------- 001000.00  100% |||||||||| method0\n" +
						"000000.00  000% ---------- 001000.00  100% |||||||||| `-- method1\n" +
						"000050.00  005% :--------- 000500.00  050% |||||-----     |-- method1_1\n" +
						"000200.00  020% ||-------- 000200.00  020% ||--------     |   |-- method1_1_1\n" +
						"000200.00  020% ||-------- 000250.00  025% ||:-------     |   `-- method1_1_2\n" +
						"000050.00  005% :--------- 000050.00  005% :---------     |       `-- method1_1_2_1\n" +
						"000150.00  015% |:-------- 000500.00  050% |||||-----     `-- method1_2\n" +
						"000050.00  005% :--------- 000050.00  005% :---------         |-- select * from user \n" +
						"000050.00  005% :--------- 000050.00  005% :---------         |-- select * from address \n" +
						"000250.00  025% ||:------- 000250.00  025% ||:-------         `-- method1_2_1\n", callStackElement.toString());
		callStackElement.recycle();
	}

	@Test
	public void testRemoveCallsFasterThan() {
		final CallStackElement callStackElement = TestClass.method0();
		callStackElement.removeCallsFasterThan(TimeUnit.MILLISECONDS.toNanos(51));
		assertEquals(
						"----------------------------------------------------------------------\n" +
						"Selftime (ms)              Total (ms)                 Method signature\n" +
						"----------------------------------------------------------------------\n" +
						"000000.00  000% ---------- 001000.00  100% |||||||||| method0\n" +
						"000000.00  000% ---------- 001000.00  100% |||||||||| `-- method1\n" +
						"000050.00  005% :--------- 000500.00  050% |||||-----     |-- method1_1\n" +
						"000200.00  020% ||-------- 000200.00  020% ||--------     |   |-- method1_1_1\n" +
						"000250.00  025% ||:------- 000250.00  025% ||:-------     |   `-- method1_1_2\n" +
						// method1_1_2_1 is excluded, because execution time 50 < 51
						"000150.00  015% |:-------- 000500.00  050% |||||-----     `-- method1_2\n" +
						// io queries are not excluded even though execution time 50 < 51
						"000050.00  005% :--------- 000050.00  005% :---------         |-- select * from user \n" +
						"000050.00  005% :--------- 000050.00  005% :---------         |-- select * from address \n" +
						"000250.00  025% ||:------- 000250.00  025% ||:-------         `-- method1_2_1\n", callStackElement.toString());
		callStackElement.recycle();
	}

	@Test
	public void testRemoveCallsFasterThanNotIncluded() {
		final CallStackElement callStackElement = TestClass.method0();
		callStackElement.removeCallsFasterThan(TimeUnit.MILLISECONDS.toNanos(50));
		assertEquals(
						"----------------------------------------------------------------------\n" +
						"Selftime (ms)              Total (ms)                 Method signature\n" +
						"----------------------------------------------------------------------\n" +
						"000000.00  000% ---------- 001000.00  100% |||||||||| method0\n" +
						"000000.00  000% ---------- 001000.00  100% |||||||||| `-- method1\n" +
						"000050.00  005% :--------- 000500.00  050% |||||-----     |-- method1_1\n" +
						"000200.00  020% ||-------- 000200.00  020% ||--------     |   |-- method1_1_1\n" +
						"000200.00  020% ||-------- 000250.00  025% ||:-------     |   `-- method1_1_2\n" +
						"000050.00  005% :--------- 000050.00  005% :---------     |       `-- method1_1_2_1\n" +
						"000150.00  015% |:-------- 000500.00  050% |||||-----     `-- method1_2\n" +
						"000050.00  005% :--------- 000050.00  005% :---------         |-- select * from user \n" +
						"000050.00  005% :--------- 000050.00  005% :---------         |-- select * from address \n" +
						"000250.00  025% ||:------- 000250.00  025% ||:-------         `-- method1_2_1\n", callStackElement.toString());
		callStackElement.recycle();
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
