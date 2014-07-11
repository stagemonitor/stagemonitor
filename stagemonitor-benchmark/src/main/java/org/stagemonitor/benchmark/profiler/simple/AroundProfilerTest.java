package org.stagemonitor.benchmark.profiler.simple;

import org.stagemonitor.requestmonitor.profiler.CallStackElement;

public class AroundProfilerTest {

	public int method1() {
		CallStackElement cse = AroundProfiler.start("public int ClassToProfile.method1()");
		try {
			return method2(1) + method3() + method5();
		} finally {
			AroundProfiler.stop(cse);
		}
	}

	private int method2(int i) {
		CallStackElement cse = AroundProfiler.start("public int ClassToProfile.method2(int i)");
		try {
			return 1 + i;
		} finally {
			AroundProfiler.stop(cse);
		}
	}

	private int method3() {
		CallStackElement cse = AroundProfiler.start("public int ClassToProfile.method3()");
		try {
			return method4();
		} finally {
			AroundProfiler.stop(cse);
		}
	}

	private int method4() {
		CallStackElement cse = AroundProfiler.start("public int ClassToProfile.method4()");
		try {
			return 4;
		} finally {
			AroundProfiler.stop(cse);
		}
	}

	private int method5() {
		CallStackElement cse = AroundProfiler.start("public int ClassToProfile.method5()");
		try {
			return method6() + method7();
		} finally {
			AroundProfiler.stop(cse);
		}
	}

	private int method6() {
		CallStackElement cse = AroundProfiler.start("public int ClassToProfile.method6()");
		try {
			return 6;
		} finally {
			AroundProfiler.stop(cse);
		}
	}

	private int method7() {
		CallStackElement cse = AroundProfiler.start("public int ClassToProfile.method7()");
		try {
			return method8();
		} finally {
			AroundProfiler.stop(cse);
		}
	}

	private int method8() {
		CallStackElement cse = AroundProfiler.start("public int ClassToProfile.method8()");
		try {
			return method9();
		} finally {
			AroundProfiler.stop(cse);
		}
	}

	private int method9() {
		CallStackElement cse = AroundProfiler.start("public int ClassToProfile.method9()");
		try {
			return 9;
		} finally {
			AroundProfiler.stop(cse);
		}
	}

}
