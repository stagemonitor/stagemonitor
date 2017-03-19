package org.stagemonitor.requestmonitor.reporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.CompletedFuture;
import org.stagemonitor.core.util.ExecutorUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.requestmonitor.tracing.wrapper.StatelessSpanEventListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

public class ReportingSpanEventListener extends StatelessSpanEventListener {

	private static final Logger logger = LoggerFactory.getLogger(ReportingSpanEventListener.class);

	private final RequestMonitorPlugin requestMonitorPlugin;
	private final ExecutorService asyncSpanReporterPool;
	private final List<SpanReporter> spanReporters = new CopyOnWriteArrayList<SpanReporter>();
	private final Configuration configuration;

	public ReportingSpanEventListener(Configuration configuration) {
		this.configuration = configuration;
		this.requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
		final int threadPoolQueueCapacityLimit = configuration.getConfig(CorePlugin.class).getThreadPoolQueueCapacityLimit();
		this.asyncSpanReporterPool = ExecutorUtils
				.createSingleThreadDeamonPool("async-request-reporter", threadPoolQueueCapacityLimit);
	}

	@Override
	public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
		final SpanContextInformation info = SpanContextInformation.forSpan(spanWrapper);
		if (monitorThisRequest() && info.isSampled()) {
			try {
				report(info);
			} catch (Exception e) {
				logger.warn(e.getMessage() + " (this exception is ignored) " + info.toString(), e);
			}
		}
	}

	private Future<?> report(final SpanContextInformation spanContext) {
		try {
			if (requestMonitorPlugin.isReportAsync()) {
				return asyncSpanReporterPool.submit(new Runnable() {
					@Override
					public void run() {
						doReport(spanContext);
					}
				});
			} else {
				doReport(spanContext);
				return new CompletedFuture<Object>(null);
			}
		} catch (RejectedExecutionException e) {
			ExecutorUtils.logRejectionWarning(e);
			return new CompletedFuture<Object>(null);
		}
	}

	private void doReport(SpanContextInformation spanContext) {
		for (SpanReporter spanReporter : spanReporters) {
			if (spanReporter.isActive(spanContext)) {
				try {
					spanReporter.report(spanContext);
				} catch (Exception e) {
					logger.warn(e.getMessage() + " (this exception is ignored)", e);
				}
			}
		}
	}

	private boolean monitorThisRequest() {
		final String msg = "This request is not monitored because {}";
		if (!Stagemonitor.isStarted()) {
			logger.debug(msg, "stagemonitor has not been started yet");
			return false;
		}
		return true;
	}


	/**
	 * TODO
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
}
