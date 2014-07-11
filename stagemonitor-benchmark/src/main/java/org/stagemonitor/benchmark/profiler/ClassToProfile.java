package org.stagemonitor.benchmark.profiler;

import org.stagemonitor.requestmonitor.profiler.Profiler;

public class ClassToProfile {


	public int method1() {
		return method2(1) + method3() + method5();
	}

	private int method2(int i) {
		return 1 + i;
	}

	private int method3() {
		return method4();
	}

	private int method4() {
		return 4;
	}

	private int method5() {
		return method6() + method7();
	}

	private int method6() {
		return 6;
	}

	private int method7() {
		return method8();
	}

	private int method8() {
		return method9();
	}

	private int method9() {
		return 9;
	}

	public static void main(String[] args) {
		final ClassToProfile classToProfile = new ClassToProfile();
		Profiler.activateProfiling("root");
		classToProfile.method1();
		classToProfile.method1();
		classToProfile.method1();
		Profiler.stop();
	}

}
