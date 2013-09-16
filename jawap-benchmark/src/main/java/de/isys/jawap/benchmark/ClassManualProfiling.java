package de.isys.jawap.benchmark;

import de.isys.jawap.collector.profile.Profiler;

public class ClassManualProfiling {
	private static final String className = "ClassManualProfiling";


	public int method1() {
		Profiler.start();
		try {
			return method2(1) + method3() + method5();
		} finally {
			Profiler.stop(className, "method1");
		}
	}

	private int method2(int i) {
		Profiler.start();
		try {
			return 1 + i;
		} finally {
			Profiler.stop(className, "method2");
		}
	}

	private int method3() {
		Profiler.start();
		try {
			return method4();
		} finally {
			Profiler.stop(className, "method3");
		}
	}

	private int method4() {
		Profiler.start();
		try {
			return 4;
		} finally {
			Profiler.stop(className, "method4");
		}
	}

	private int method5() {
		Profiler.start();
		try {
			return method6() + method7();
		} finally {
			Profiler.stop(className, "method5");
		}
	}

	private int method6() {
		Profiler.start();
		try {
			return 6;
		} finally {
			Profiler.stop(className, "method6");
		}
	}

	private int method7() {
		Profiler.start();
		try {
			return 7;
		} finally {
			Profiler.stop(className, "method7");
		}
	}

}
