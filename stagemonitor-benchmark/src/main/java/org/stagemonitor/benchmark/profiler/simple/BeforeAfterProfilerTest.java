package org.stagemonitor.benchmark.profiler.simple;

public class BeforeAfterProfilerTest {

	public int method1() {
		BeforeAfterProfiler.start("public int ClassToProfile.method1()");
		try {
			return method2(1) + method3() + method5();
		} finally {
			BeforeAfterProfiler.stop();
		}
	}

	private int method2(int i) {
		BeforeAfterProfiler.start("public int ClassToProfile.method2(int i)");
		try {
			return 1 + i;
		} finally {
			BeforeAfterProfiler.stop();
		}
	}

	private int method3() {
		BeforeAfterProfiler.start("public int ClassToProfile.method3()");
		try {
			return method4();
		} finally {
			BeforeAfterProfiler.stop();
		}
	}

	private int method4() {
		BeforeAfterProfiler.start("public int ClassToProfile.method4()");
		try {
			return 4;
		} finally {
			BeforeAfterProfiler.stop();
		}
	}

	private int method5() {
		BeforeAfterProfiler.start("public int ClassToProfile.method5()");
		try {
			return method6() + method7();
		} finally {
			BeforeAfterProfiler.stop();
		}
	}

	private int method6() {
		BeforeAfterProfiler.start("public int ClassToProfile.method6()");
		try {
			return 6;
		} finally {
			BeforeAfterProfiler.stop();
		}
	}

	private int method7() {
		BeforeAfterProfiler.start("public int ClassToProfile.method7()");
		try {
			return method8();
		} finally {
			BeforeAfterProfiler.stop();
		}
	}

	private int method8() {
		BeforeAfterProfiler.start("public int ClassToProfile.method8()");
		try {
			return method9();
		} finally {
			BeforeAfterProfiler.stop();
		}
	}

	private int method9() {
		BeforeAfterProfiler.start("public int ClassToProfile.method9()");
		try {
			return 9;
		} finally {
			BeforeAfterProfiler.stop();
		}
	}

}
