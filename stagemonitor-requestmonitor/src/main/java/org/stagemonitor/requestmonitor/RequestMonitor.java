package org.stagemonitor.requestmonitor;

import com.uber.jaeger.context.TraceContext;
import com.uber.jaeger.context.TracingUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.CompletedFuture;
import org.stagemonitor.core.util.ExecutorUtils;
import org.stagemonitor.requestmonitor.reporter.SpanReporter;
import org.stagemonitor.requestmonitor.tracing.NoopSpan;
import org.stagemonitor.requestmonitor.tracing.wrapper.AbstractSpanEventListener;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import io.opentracing.Span;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

/**
 * @deprecated we should try to do everything with {@link AbstractSpanEventListener}s
 */
@Deprecated
public class RequestMonitor {

	private static final Logger logger = LoggerFactory.getLogger(RequestMonitor.class);

	private final List<SpanReporter> spanReporters = new CopyOnWriteArrayList<SpanReporter>();

	private final MetricName internalOverheadMetricName = name("internal_overhead_request_monitor").build();

	private ExecutorService asyncSpanReporterPool;

	private final Configuration configuration;
	private Metric2Registry metricRegistry;
	private CorePlugin corePlugin;
	private RequestMonitorPlugin requestMonitorPlugin;

	public RequestMonitor(Configuration configuration, Metric2Registry registry) {
		this(configuration, registry, ServiceLoader.load(SpanReporter.class, RequestMonitor.class.getClassLoader()));
	}

	public RequestMonitor(Configuration configuration, Metric2Registry registry, Iterable<SpanReporter> spanReporters) {
		this(configuration, registry, configuration.getConfig(RequestMonitorPlugin.class), spanReporters);
	}

	private RequestMonitor(Configuration configuration, Metric2Registry registry, RequestMonitorPlugin requestMonitorPlugin,
						   Iterable<SpanReporter> spanReporters) {
		this.configuration = configuration;
		this.metricRegistry = registry;
		this.corePlugin = configuration.getConfig(CorePlugin.class);
		this.requestMonitorPlugin = requestMonitorPlugin;
		this.asyncSpanReporterPool = ExecutorUtils
				.createSingleThreadDeamonPool("async-request-reporter", corePlugin.getThreadPoolQueueCapacityLimit());
		for (SpanReporter spanReporter : spanReporters) {
			addReporter(spanReporter);
		}
	}

	public void monitorStart(MonitoredRequest monitoredRequest) {
		final long start = System.nanoTime();
		try {
			if (!corePlugin.isStagemonitorActive()) {
				return;
			}

			if (Stagemonitor.getMeasurementSession().isNull()) {
				createMeasurementSession();
			}

			if (Stagemonitor.getMeasurementSession().getInstanceName() == null) {
				getInstanceNameFromExecution(monitoredRequest);
			}

			if (!Stagemonitor.isStarted()) {
				Stagemonitor.startMonitoring();
			}
			if (monitorThisRequest()) {
				monitoredRequest.createSpan();
			}
		} finally {
			final SpanContextInformation info = SpanContextInformation.getCurrent();
			if (info != null) {
				info.setOverhead1(System.nanoTime() - start);
			}
		}
	}

	public void monitorStop() {
		final SpanContextInformation info = SpanContextInformation.getCurrent();
		if (info == null || !corePlugin.isStagemonitorActive()) {
			return;
		}
		long overhead2 = System.nanoTime();
		if (info.getSpan() != null) {
			info.getSpan().finish();
		}
		if (monitorThisRequest() && info.isSampled()) {
			try {
				info.setSpanReporterFuture(report(info));
			} catch (Exception e) {
				logger.warn(e.getMessage() + " (this exception is ignored) " + info.toString(), e);
			}
		}

		trackOverhead(info.getOverhead1(), overhead2);
	}

	public SpanContextInformation monitor(MonitoredRequest monitoredRequest) throws Exception {
		try {
			monitorStart(monitoredRequest);
			monitoredRequest.execute();
			return SpanContextInformation.getCurrent();
		} catch (Exception e) {
			recordException(e);
			throw e;
		} finally {
			monitorStop();
		}
	}

	public void recordException(Exception e) {
		SpanUtils.setException(getSpan(), e, requestMonitorPlugin.getIgnoreExceptions(), requestMonitorPlugin.getUnnestExceptions());
	}

	private void trackOverhead(long overhead1, long overhead2) {
		if (corePlugin.isInternalMonitoringActive()) {
			overhead2 = System.nanoTime() - overhead2;
			metricRegistry.timer(internalOverheadMetricName).update(overhead2 + overhead1, NANOSECONDS);
		}
	}

	/*
	 * In case the instance name is not set by configuration, try to read from monitored execution
	 * (e.g. the domain name from a HTTP request)
	 */
	private synchronized void getInstanceNameFromExecution(MonitoredRequest monitoredRequest) {
		final MeasurementSession measurementSession = Stagemonitor.getMeasurementSession();
		if (measurementSession.getInstanceName() == null) {
			MeasurementSession session = new MeasurementSession(measurementSession.getApplicationName(), measurementSession.getHostName(),
					monitoredRequest.getInstanceName());
			Stagemonitor.setMeasurementSession(session);
		}
	}

	private synchronized void createMeasurementSession() {
		if (Stagemonitor.getMeasurementSession().isNull()) {
			MeasurementSession session = new MeasurementSession(corePlugin.getApplicationName(), corePlugin.getHostName(),
					corePlugin.getInstanceName());
			Stagemonitor.setMeasurementSession(session);
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
	 * @return the {@link Span} of the current request or a noop {@link Span} (never <code>null</code>)
	 */
	public Span getSpan() {
		final TraceContext traceContext = TracingUtils.getTraceContext();
		if (!traceContext.isEmpty()) {
			return traceContext.getCurrentSpan();
		} else {
			return NoopSpan.INSTANCE;
		}
	}

	/**
	 * Adds a {@link SpanReporter}
	 *
	 * @param spanReporter the {@link SpanReporter} to add
	 */
	public static void addSpanReporter(SpanReporter spanReporter) {
		get().addReporter(spanReporter);
	}

	/**
	 * Gets the {@link RequestMonitor}.
	 * <p/>
	 * You can use this instance for example to call the following methods:
	 * <ul>
	 * <li>{@link #addReporter(SpanReporter)}</li>
	 * <li>{@link #getSpan()}</li>
	 * </ul>
	 *
	 * @return the current request monitor
	 */
	public static RequestMonitor get() {
		return Stagemonitor.getPlugin(RequestMonitorPlugin.class).getRequestMonitor();
	}

	/**
	 * Adds a {@link SpanReporter}
	 *
	 * @param spanReporter the {@link SpanReporter} to add
	 */
	public void addReporter(SpanReporter spanReporter) {
		spanReporters.add(0, spanReporter);
		spanReporter.init(new SpanReporter.InitArguments(configuration, metricRegistry));
	}

	/**
	 * Shuts down the internal thread pool
	 */
	public void close() {
		asyncSpanReporterPool.shutdown();
	}

}
