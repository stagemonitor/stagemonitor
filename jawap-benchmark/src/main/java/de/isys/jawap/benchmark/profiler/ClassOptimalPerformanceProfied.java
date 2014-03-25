package de.isys.jawap.benchmark.profiler;


public class ClassOptimalPerformanceProfied {

	public int method1() {
		OptimalPerformanceProfilerMock.start();
		try {
			return method2(1) + method3() + method5();
		} finally {
			OptimalPerformanceProfilerMock.stop("public int de.isys.jawap.benchmark.profiler.ClassToProfile.method1()");
		}
	}

	private int method2(int i) {
		OptimalPerformanceProfilerMock.start();
		try {
			return 1 + i;
		} finally {
			OptimalPerformanceProfilerMock.stop("public int de.isys.jawap.benchmark.profiler.ClassToProfile.method2(int i)");
		}
	}

	private int method3() {
		OptimalPerformanceProfilerMock.start();
		try {
			return method4();
		} finally {
			OptimalPerformanceProfilerMock.stop("public int de.isys.jawap.benchmark.profiler.ClassToProfile.method3()");
		}
	}

	private int method4() {
		OptimalPerformanceProfilerMock.start();
		try {
			return 4;
		} finally {
			OptimalPerformanceProfilerMock.stop("public int de.isys.jawap.benchmark.profiler.ClassToProfile.method4()");
		}
	}

	private int method5() {
		OptimalPerformanceProfilerMock.start();
		try {
			return method6() + method7();
		} finally {
			OptimalPerformanceProfilerMock.stop("public int de.isys.jawap.benchmark.profiler.ClassToProfile.method5()");
		}
	}

	private int method6() {
		OptimalPerformanceProfilerMock.start();
		try {
			return 6;
		} finally {
			OptimalPerformanceProfilerMock.stop("public int de.isys.jawap.benchmark.profiler.ClassToProfile.method6()");
		}
	}

	private int method7() {
		OptimalPerformanceProfilerMock.start();
		try {
			return method8();
		} finally {
			OptimalPerformanceProfilerMock.stop("public int de.isys.jawap.benchmark.profiler.ClassToProfile.method7()");
		}
	}

	private int method8() {
		OptimalPerformanceProfilerMock.start();
		try {
			return method9();
		} finally {
			OptimalPerformanceProfilerMock.stop("public int de.isys.jawap.benchmark.profiler.ClassToProfile.method8()");
		}
	}

	private int method9() {
		OptimalPerformanceProfilerMock.start();
		try {
			return 9;
		} finally {
			OptimalPerformanceProfilerMock.stop("public int de.isys.jawap.benchmark.profiler.ClassToProfile.method9()");
		}
	}

}
