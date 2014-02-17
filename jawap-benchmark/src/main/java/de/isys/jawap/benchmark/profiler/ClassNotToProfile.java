package de.isys.jawap.benchmark.profiler;

public class ClassNotToProfile {


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
}
