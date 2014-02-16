package de.isys.jawap.benchmark.profiler;

import de.isys.jawap.collector.profiler.Profiler;

public class ClassManualProfiling {

	public int method1() {
		Profiler.start();
		try {
			return method2(1) + method3() + method5();
		} finally {
			Profiler.stop("public int de.isys.jawap.benchmark.profiler.ClassToProfile.method1()");
		}
	}

	private int method2(int i) {
		Profiler.start();
		try {
			return 1 + i;
		} finally {
			Profiler.stop("public int de.isys.jawap.benchmark.profiler.ClassToProfile.method2(int i)");
		}
	}

	private int method3() {
		Profiler.start();
		try {
			return method4();
		} finally {
			Profiler.stop("public int de.isys.jawap.benchmark.profiler.ClassToProfile.method3()");
		}
	}

	private int method4() {
		Profiler.start();
		try {
			return 4;
		} finally {
			Profiler.stop("public int de.isys.jawap.benchmark.profiler.ClassToProfile.method4()");
		}
	}

	private int method5() {
		Profiler.start();
		try {
			return method6() + method7();
		} finally {
			Profiler.stop("public int de.isys.jawap.benchmark.profiler.ClassToProfile.method5()");
		}
	}

	private int method6() {
		Profiler.start();
		try {
			return 6;
		} finally {
			Profiler.stop("public int de.isys.jawap.benchmark.profiler.ClassToProfile.method6()");
		}
	}

	private int method7() {
		Profiler.start();
		try {
			return 7;
		} finally {
			Profiler.stop("public int de.isys.jawap.benchmark.profiler.ClassToProfile.method7()");
		}
	}

	public static void main(String[] args) {
		new ClassManualProfiling().method1();
	}

}
