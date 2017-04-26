package org.stagemonitor.benchmark.profiler;

import org.stagemonitor.tracing.profiler.Profiler;

public class ClassManualProfiling {

	public int method1() {
		Profiler.start("public int ClassToProfile.method1()");
		try {
			return method2(1) + method3() + method5();
		} finally {
			Profiler.stop();
		}
	}

	private int method2(int i) {
		Profiler.start("public int ClassToProfile.method2(int i)");
		try {
			return 1 + i;
		} finally {
			Profiler.stop();
		}
	}

	private int method3() {
		Profiler.start("public int ClassToProfile.method3()");
		try {
			return method4();
		} finally {
			Profiler.stop();
		}
	}

	private int method4() {
		Profiler.start("public int ClassToProfile.method4()");
		try {
			return 4;
		} finally {
			Profiler.stop();
		}
	}

	private int method5() {
		Profiler.start("public int ClassToProfile.method5()");
		try {
			return method6() + method7();
		} finally {
			Profiler.stop();
		}
	}

	private int method6() {
		Profiler.start("public int ClassToProfile.method6()");
		try {
			return 6;
		} finally {
			Profiler.stop();
		}
	}

	private int method7() {
		Profiler.start("public int ClassToProfile.method7()");
		try {
			return method8();
		} finally {
			Profiler.stop();
		}
	}

	private int method8() {
		Profiler.start("public int ClassToProfile.method8()");
		try {
			return method9();
		} finally {
			Profiler.stop();
		}
	}

	private int method9() {
		Profiler.start("public int ClassToProfile.method9()");
		try {
			return 9;
		} finally {
			Profiler.stop();
		}
	}

	public static void main(String[] args) {
		new ClassManualProfiling().method1();
	}

}
