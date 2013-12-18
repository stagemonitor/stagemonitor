package de.isys.jawap.benchmark;

import de.isys.jawap.collector.profiler.Profiler;

public class ClassManualProfiling {
	private static final String className = "ClassManualProfiling";


	public int method1() {
		Profiler.start();
		try {
			return method2(1) + method3() + method5();
		} finally {
			Profiler.stop(className, "public int method1()");
		}
	}

	private int method2(int i) {
		Profiler.start();
		try {
			return 1 + i;
		} finally {
			Profiler.stop(className, "public int method2(int i)");
		}
	}

	private int method3() {
		Profiler.start();
		try {
			return method4();
		} finally {
			Profiler.stop(className, "public int method3()");
		}
	}

	private int method4() {
		Profiler.start();
		try {
			return 4;
		} finally {
			Profiler.stop(className, "public int method4()");
		}
	}

	private int method5() {
		Profiler.start();
		try {
			return method6() + method7();
		} finally {
			Profiler.stop(className, "public int method5()");
		}
	}

	private int method6() {
		Profiler.start();
		try {
			return 6;
		} finally {
			Profiler.stop(className, "public int method6()");
		}
	}

	private int method7() {
		Profiler.start();
		try {
			return 7;
		} finally {
			Profiler.stop(className, "public int method7()");
		}
	}

}
