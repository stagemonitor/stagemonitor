package org.stagemonitor.tracing.reporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.util.CompletedFuture;
import org.stagemonitor.core.util.ExecutorUtils;
import org.stagemonitor.tracing.B3HeaderFormat;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.tracing.wrapper.StatelessSpanEventListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

public class ReportingSpanEventListener extends StatelessSpanEventListener {

	private static final Logger logger = LoggerFactory.getLogger(ReportingSpanEventListener.class);

	private final TracingPlugin tracingPlugin;
	private final ExecutorService asyncSpanReporterPool;
	private final List<SpanReporter> spanReporters = new CopyOnWriteArrayList<SpanReporter>();
	private final ConfigurationRegistry configuration;

	public ReportingSpanEventListener(ConfigurationRegistry configuration) {
		this.configuration = configuration;
		this.tracingPlugin = configuration.getConfig(TracingPlugin.class);
		final CorePlugin corePlugin = configuration.getConfig(CorePlugin.class);
		this.asyncSpanReporterPool = ExecutorUtils
				.createSingleThreadDeamonPool("async-request-reporter", corePlugin.getThreadPoolQueueCapacityLimit(), corePlugin);
	}

	@Override
	public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
		final SpanContextInformation info = SpanContextInformation.forSpan(spanWrapper);
		if (tracingPlugin.isSampled(spanWrapper)) {
			try {
				report(info, spanWrapper);
			} catch (Exception e) {
				logger.warn(e.getMessage() + " (this exception is ignored) " + info.toString(), e);
			}
		}
	}

	private Future<?> report(final SpanContextInformation spanContext, final SpanWrapper spanWrapper) {
		try {
			if (tracingPlugin.isReportAsync()) {
				return asyncSpanReporterPool.submit(new Runnable() {
					@Override
					public void run() {
						doReport(spanContext, spanWrapper);
					}
				});
			} else {
				doReport(spanContext, spanWrapper);
				return new CompletedFuture<Object>(null);
			}
		} catch (RejectedExecutionException e) {
			ExecutorUtils.logRejectionWarning(e);
			return new CompletedFuture<Object>(null);
		}
	}

	private void doReport(SpanContextInformation spanContext, SpanWrapper spanWrapper) {
		for (SpanReporter spanReporter : spanReporters) {
			if (spanReporter.isActive(spanContext)) {
				try {
					spanReporter.report(spanContext, spanWrapper);
				} catch (Exception e) {
					logger.warn(e.getMessage() + " (this exception is ignored)", e);
				}
			}
		}
	}

	/**
	 * Shuts down the internal thread pool
	 */
	public void close() {
		asyncSpanReporterPool.shutdown();
	}

	/**
	 * Adds a {@link SpanReporter}
	 *
	 * @param spanReporter the {@link SpanReporter} to add
	 */
	public void addReporter(SpanReporter spanReporter) {
		spanReporters.add(0, spanReporter);
		spanReporter.init(configuration);
	}

	public void update(B3HeaderFormat.B3Identifiers spanIdentifiers, B3HeaderFormat.B3Identifiers newSpanIdentifiers, Map<String, Object> tags) {
		for (SpanReporter spanReporter : spanReporters) {
			try {
				spanReporter.updateSpan(spanIdentifiers, newSpanIdentifiers, tags);
			} catch (Exception e) {
				logger.warn(e.getMessage() + " (this exception is ignored)", e);
			}
		}
	}
}
