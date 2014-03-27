package org.stagemonitor.benchmark.profiler.simple;

public class BeforeAfterProfilerTest {

	public int method1() {
		BeforeAfterProfiler.start();
		try {
			return method2(1) + method3() + method5();
		} finally {
			BeforeAfterProfiler.stop("public int ClassToProfile.method1()");
		}
	}

	private int method2(int i) {
		BeforeAfterProfiler.start();
		try {
			return 1 + i;
		} finally {
			BeforeAfterProfiler.stop("public int ClassToProfile.method2(int i)");
		}
	}

	private int method3() {
		BeforeAfterProfiler.start();
		try {
			return method4();
		} finally {
			BeforeAfterProfiler.stop("public int ClassToProfile.method3()");
		}
	}

	private int method4() {
		BeforeAfterProfiler.start();
		try {
			return 4;
		} finally {
			BeforeAfterProfiler.stop("public int ClassToProfile.method4()");
		}
	}

	private int method5() {
		BeforeAfterProfiler.start();
		try {
			return method6() + method7();
		} finally {
			BeforeAfterProfiler.stop("public int ClassToProfile.method5()");
		}
	}

	private int method6() {
		BeforeAfterProfiler.start();
		try {
			return 6;
		} finally {
			BeforeAfterProfiler.stop("public int ClassToProfile.method6()");
		}
	}

	private int method7() {
		BeforeAfterProfiler.start();
		try {
			return method8();
		} finally {
			BeforeAfterProfiler.stop("public int ClassToProfile.method7()");
		}
	}

	private int method8() {
		BeforeAfterProfiler.start();
		try {
			return method9();
		} finally {
			BeforeAfterProfiler.stop("public int ClassToProfile.method8()");
		}
	}

	private int method9() {
		BeforeAfterProfiler.start();
		try {
			return 9;
		} finally {
			BeforeAfterProfiler.stop("public int ClassToProfile.method9()");
		}
	}

}
