package de.isys.jawap.benchmark.profiler;

import java.util.ArrayList;
import java.util.List;

public class OptimalPerformanceProfilerMock {

	static List<Long> times = new ArrayList<Long>();
	static List<String> signatures = new ArrayList<String>();
	private static long dummy = 0;

	public static void start() {
		dummy = System.nanoTime();
	}
	public static void stop(String signature) {
		dummy = dummy - System.nanoTime();
		times.add(dummy);
		signatures.add(signature);
	}
}
