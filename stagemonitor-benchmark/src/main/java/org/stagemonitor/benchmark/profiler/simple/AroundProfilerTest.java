package org.stagemonitor.benchmark.profiler.simple;

import org.stagemonitor.collector.profiler.CallStackElement;

public class AroundProfilerTest {

	public int method1() {
		CallStackElement cse = AroundProfiler.start();
		try {
			return method2(1) + method3() + method5();
		} finally {
			AroundProfiler.stop(cse, "public int ClassToProfile.method1()");
		}
	}

	private int method2(int i) {
		CallStackElement cse = AroundProfiler.start();
		try {
			return 1 + i;
		} finally {
			AroundProfiler.stop(cse, "public int ClassToProfile.method2(int i)");
		}
	}

	private int method3() {
		CallStackElement cse = AroundProfiler.start();
		try {
			return method4();
		} finally {
			AroundProfiler.stop(cse, "public int ClassToProfile.method3()");
		}
	}

	private int method4() {
		CallStackElement cse = AroundProfiler.start();
		try {
			return 4;
		} finally {
			AroundProfiler.stop(cse, "public int ClassToProfile.method4()");
		}
	}

	private int method5() {
		CallStackElement cse = AroundProfiler.start();
		try {
			return method6() + method7();
		} finally {
			AroundProfiler.stop(cse, "public int ClassToProfile.method5()");
		}
	}

	private int method6() {
		CallStackElement cse = AroundProfiler.start();
		try {
			return 6;
		} finally {
			AroundProfiler.stop(cse, "public int ClassToProfile.method6()");
		}
	}

	private int method7() {
		CallStackElement cse = AroundProfiler.start();
		try {
			return method8();
		} finally {
			AroundProfiler.stop(cse, "public int ClassToProfile.method7()");
		}
	}

	private int method8() {
		CallStackElement cse = AroundProfiler.start();
		try {
			return method9();
		} finally {
			AroundProfiler.stop(cse, "public int ClassToProfile.method8()");
		}
	}

	private int method9() {
		CallStackElement cse = AroundProfiler.start();
		try {
			return 9;
		} finally {
			AroundProfiler.stop(cse, "public int ClassToProfile.method9()");
		}
	}

	public static void main(String[] args) {
		final CallStackElement root = new CallStackElement();
		AroundProfiler.setMethodCallRoot(root);
		new AroundProfilerTest().method1();
		System.out.println(root.getChildren().get(0));
	}

}
