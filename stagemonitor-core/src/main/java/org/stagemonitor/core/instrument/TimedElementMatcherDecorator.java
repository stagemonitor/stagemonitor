package org.stagemonitor.core.instrument;

import com.codahale.metrics.Counter;

import net.bytebuddy.matcher.ElementMatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.SortedTableLogReporter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

import java.util.concurrent.TimeUnit;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class TimedElementMatcherDecorator<T> implements ElementMatcher<T> {

	private static final Logger logger = LoggerFactory.getLogger(TimedElementMatcherDecorator.class);
	private static final boolean DEBUG_INSTRUMENTATION = Stagemonitor.getConfiguration().getConfig(CorePlugin.class).isDebugInstrumentation();

	private static final Metric2Registry timeRegistry = new Metric2Registry();
	private static final Metric2Registry countRegistry = new Metric2Registry();
	private final ElementMatcher<T> delegate;

	private final Counter count;
	private final Counter time;

	public static <T> ElementMatcher<T> timed(String type, String transformerName, ElementMatcher<T> delegate) {
		if (DEBUG_INSTRUMENTATION) {
			return new TimedElementMatcherDecorator<T>(delegate, type, transformerName);
		} else {
			return delegate;
		}
	}

	private TimedElementMatcherDecorator(ElementMatcher<T> delegate, String type, String transformerName) {
		this.delegate = delegate;
		this.count = countRegistry
				.counter(name("element_matcher").type(type).tag("transformer", transformerName).build());
		this.time = timeRegistry
				.counter(name("element_matcher").type(type).tag("transformer", transformerName).build());
	}

	@Override
	public boolean matches(T target) {
		long start = System.nanoTime();
		try {
			return delegate.matches(target);
		} finally {
			count.inc();
			time.inc(System.nanoTime() - start);
		}
	}

	public static void logMetrics() {
		if (DEBUG_INSTRUMENTATION) {
			logger.info("ElementMatcher TIME (nanoseconds total)");
			SortedTableLogReporter.forRegistry(timeRegistry).convertDurationsTo(TimeUnit.MICROSECONDS).build().report();
			logger.info("ElementMatcher COUNT");
			SortedTableLogReporter.forRegistry(countRegistry).convertDurationsTo(TimeUnit.MICROSECONDS).build().report();

			long totalTime = 0;
			for (Counter counter : timeRegistry.getCounters().values()) {
				totalTime += counter.getCount();
			}
			logger.info("Total time: {} ms", TimeUnit.NANOSECONDS.toMillis(totalTime));
		}
	}
}
