package de.isys.jawap.benchmark.profiler;

import de.isys.jawap.collector.profiler.Profiler;
import de.isys.jawap.entities.web.HttpExecutionContext;

public class ClassJavassistProfiled {


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
		return 7;
	}

	public static void main(String[] args) {
		HttpExecutionContext executionContext = new HttpExecutionContext();
		Profiler.setExecutionContext(executionContext);
		new ClassJavassistProfiled().method1();
		System.out.println(executionContext);
	}

}
