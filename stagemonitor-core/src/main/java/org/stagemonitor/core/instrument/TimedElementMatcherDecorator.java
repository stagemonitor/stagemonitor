package org.stagemonitor.core.instrument;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.metrics.SortedTableLogReporter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

public class TimedElementMatcherDecorator<T> implements ElementMatcher<T> {

	private static final Logger logger = LoggerFactory.getLogger(TimedElementMatcherDecorator.class);

	private static final Metric2Registry timeRegistry = new Metric2Registry();
	private static final Metric2Registry countRegistry = new Metric2Registry();
	private final ElementMatcher<? super T> delegate;

	private final Counter count;
	private final Counter time;

	public static <T> TimedElementMatcherDecorator<T> timed(ElementMatcher<? super T> delegate, String type, String transformerName) {
		return new TimedElementMatcherDecorator<T>(delegate, type, transformerName);
	}

	private TimedElementMatcherDecorator(ElementMatcher<? super T> delegate, String type, String transformerName) {
		this.delegate = delegate;
		this.count = timeRegistry
				.counter(name("element_matcher").type(type).tag("transformer", transformerName).build());
		this.time = countRegistry
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
		logger.info("TIME");
		SortedTableLogReporter.forRegistry(timeRegistry).convertDurationsTo(TimeUnit.MICROSECONDS).build().report();
		logger.info("COUNT");
		SortedTableLogReporter.forRegistry(countRegistry).convertDurationsTo(TimeUnit.MICROSECONDS).build().report();

		long totalTime = 0;
		for (Counter counter : timeRegistry.getCounters().values()) {
			totalTime += counter.getCount();
		}
		logger.info("Total time: {} ms", TimeUnit.NANOSECONDS.toMillis(totalTime));
	}
}
