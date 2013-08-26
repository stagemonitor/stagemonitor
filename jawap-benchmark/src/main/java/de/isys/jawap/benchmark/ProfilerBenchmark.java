package de.isys.jawap.benchmark;

import de.isys.jawap.collector.model.HttpRequestStats;
import de.isys.jawap.collector.model.MethodCallStats;
import de.isys.jawap.collector.profile.Profiler;
import de.isys.jawap.collector.service.DefaultPerformanceMeasuringService;
import de.isys.jawap.collector.service.PerformanceMeasuringService;

import java.util.Collections;

public class ProfilerBenchmark {

	private ClassToProfile classToProfile = new ClassToProfile();
	private PerformanceMeasuringService performanceMeasuringService = new DefaultPerformanceMeasuringService();

	public void testAspectJProfiler() {
		HttpRequestStats httpRequestStats = new HttpRequestStats();
		MethodCallStats root = new MethodCallStats(null);
		httpRequestStats.setMethodCallStats(Collections.singletonList(root));
		Profiler.setMethodCallRoot(root);
		classToProfile.method1();
		performanceMeasuringService.logStats(httpRequestStats);
	}

	public static void main(String[] args) {
		ProfilerBenchmark profilerBenchmark = new ProfilerBenchmark();
		profilerBenchmark.testAspectJProfiler();
	}
}
