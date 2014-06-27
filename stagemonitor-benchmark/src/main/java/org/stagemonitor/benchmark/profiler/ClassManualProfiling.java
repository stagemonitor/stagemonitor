package org.stagemonitor.benchmark.profiler;

import org.stagemonitor.requestmonitor.profiler.Profiler;

public class ClassManualProfiling {

	public int method1() {
		Profiler.start();
		try {
			return method2(1) + method3() + method5();
		} finally {
			Profiler.stop("public int ClassToProfile.method1()");
		}
	}

	private int method2(int i) {
		Profiler.start();
		try {
			return 1 + i;
		} finally {
			Profiler.stop("public int ClassToProfile.method2(int i)");
		}
	}

	private int method3() {
		Profiler.start();
		try {
			return method4();
		} finally {
			Profiler.stop("public int ClassToProfile.method3()");
		}
	}

	private int method4() {
		Profiler.start();
		try {
			return 4;
		} finally {
			Profiler.stop("public int ClassToProfile.method4()");
		}
	}

	private int method5() {
		Profiler.start();
		try {
			return method6() + method7();
		} finally {
			Profiler.stop("public int ClassToProfile.method5()");
		}
	}

	private int method6() {
		Profiler.start();
		try {
			return 6;
		} finally {
			Profiler.stop("public int ClassToProfile.method6()");
		}
	}

	private int method7() {
		Profiler.start();
		try {
			return method8();
		} finally {
			Profiler.stop("public int ClassToProfile.method7()");
		}
	}

	private int method8() {
		Profiler.start();
		try {
			return method9();
		} finally {
			Profiler.stop("public int ClassToProfile.method8()");
		}
	}

	private int method9() {
		Profiler.start();
		try {
			return 9;
		} finally {
			Profiler.stop("public int ClassToProfile.method9()");
		}
	}

	public static void main(String[] args) {
		new ClassManualProfiling().method1();
	}

}
