package org.stagemonitor.requestmonitor;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.core.util.CompletedFuture;
import org.stagemonitor.core.util.ExecutorUtils;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;
import org.stagemonitor.requestmonitor.reporter.SpanReporter;
import org.stagemonitor.requestmonitor.tracing.NoopSpan;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanInterceptor;
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
import static org.stagemonitor.requestmonitor.metrics.ServerRequestMetricsSpanInterceptor.getTimerMetricName;

/**
 * @deprecated we should try to do everything with {@link SpanInterceptor}s
 */
@Deprecated
public class RequestMonitor {

	private static final Logger logger = LoggerFactory.getLogger(RequestMonitor.class);

	// TODO remove static keyword. This is currently needed for tests
	private static ThreadLocal<SpanContextInformation> request = new ThreadLocal<SpanContextInformation>();

	private final List<SpanReporter> spanReporters = new CopyOnWriteArrayList<SpanReporter>();

	private final MetricName internalOverheadMetricName = name("internal_overhead_request_monitor").build();

	private ExecutorService asyncSpanReporterPool;

	private final Configuration configuration;
	private Metric2Registry metricRegistry;
	private CorePlugin corePlugin;
	private RequestMonitorPlugin requestMonitorPlugin;
	private Meter callTreeMeter = new Meter();

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
		SpanContextInformation info = new SpanContextInformation();
		info.setMonitoredRequest(monitoredRequest);
		info.setParent(request.get());
		request.set(info);
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
				beforeExecution(monitoredRequest, info);
			}
		} finally {
			info.setOverhead1(System.nanoTime() - start);
		}
	}

	public void monitorStop() {
		final SpanContextInformation info = getSpanContext();
		if (!corePlugin.isStagemonitorActive()) {
			cleanUpAfter(info);
			return;
		}
		long overhead2 = System.nanoTime();
		info.getMonitoredRequest().onPostExecute(info);
		if (info.getSpan() != null) {
			info.getSpan().finish();
		}
		if (monitorThisRequest() && info.isReport()) {
			try {
				info.setSpanReporterFuture(report(info));
			} catch (Exception e) {
				logger.warn(e.getMessage() + " (this exception is ignored) " + info.toString(), e);
			}
		} else {
			removeTimerIfCountIsZero(info);
		}

		cleanUpAfter(info);

		trackOverhead(info.getOverhead1(), overhead2);
	}

	private void cleanUpAfter(SpanContextInformation info) {
		request.set(info.getParent());
		if (info.getCallTree() != null) {
			Profiler.clearMethodCallParent();
		}
	}

	public SpanContextInformation monitor(MonitoredRequest monitoredRequest) throws Exception {
		try {
			monitorStart(monitoredRequest);
			monitoredRequest.execute();
			return getSpanContext();
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

	private void beforeExecution(MonitoredRequest monitoredRequest, SpanContextInformation info) {
		monitoredRequest.createSpan(info);
		try {
			if (isProfileThisRequest(info)) {
				callTreeMeter.mark();
				info.setCallTree(Profiler.activateProfiling("total"));
			}
		} catch (RuntimeException e) {
			logger.warn(e.getMessage() + " (this exception is ignored) " + info.toString(), e);
		}
	}

	private void removeTimerIfCountIsZero(SpanContextInformation info) {
		final String requestName = info.getOperationName();
		if (requestName != null) {
			MetricName timerMetricName = getTimerMetricName(requestName);
			final Metric timer = metricRegistry.getMetrics().get(timerMetricName);
			if (timer instanceof Timer && ((Timer) timer).getCount() == 0) {
				metricRegistry.remove(timerMetricName);
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
		spanContext.getMonitoredRequest().onBeforeReport(spanContext);
		for (SpanReporter spanReporter : spanReporters) {
			final CallStackElement callTree = spanContext.getCallTree();
			if (isActive(spanContext, spanReporter)) {
				try {
					spanReporter.report(spanContext);
				} catch (Exception e) {
					logger.warn(e.getMessage() + " (this exception is ignored)", e);
				}
			}
			if (callTree != null) {
				callTree.recycle();
			}
		}
	}

	public SpanContextInformation getSpanContext() {
		return request.get();
	}

	private boolean monitorThisRequest() {
		final String msg = "This request is not monitored because {}";
		if (!Stagemonitor.isStarted()) {
			logger.debug(msg, "stagemonitor has not been started yet");
			return false;
		}
		return true;
	}

	private boolean isProfileThisRequest(SpanContextInformation spanContext) {
		if (spanContext.getSpan() == null || spanContext.isExternalRequest()) {
			return false;
		}
		double callTreeRateLimit = requestMonitorPlugin.getOnlyCollectNCallTreesPerMinute();
		if (!requestMonitorPlugin.isProfilerActive()) {
			logger.debug("Not profiling this request because stagemonitor.profiler.active=false");
			return false;
		} else if (callTreeRateLimit <= 0) {
			logger.debug("Not profiling this request because stagemonitor.requestmonitor.onlyReportNRequestsPerMinuteToElasticsearch <= 0");
			return false;
		} else if (!isAnySpanReporterActive(spanContext)) {
			// TODO what if no span reporter is active but for example the jaeger zikin reporter is active?
			logger.debug("Not profiling this request because no SpanReporter is active {}", spanReporters);
			return false;
		} else if (callTreeRateLimit < 1000000d && callTreeMeter.getOneMinuteRate() >= callTreeRateLimit) {
			logger.debug("Not profiling this request because more than {} call trees per minute where created", callTreeRateLimit);
			return false;
		} else if (!spanContext.isReport()) {
			logger.debug("Not profiling this request because this request is not sampled", callTreeRateLimit);
			return false;
		}
		return true;
	}

	private boolean isAnySpanReporterActive(SpanContextInformation spanContext) {
		for (SpanReporter reporter : spanReporters) {
			if (isActive(spanContext, reporter)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether the given {@link SpanReporter} is active for the current {@link Span}. If this method was
	 * already called for a {@link SpanReporter} in the context of the current request it returns the previous result.
	 * In other words this method makes sure that {@link SpanReporter#isActive(SpanContextInformation)} is
	 * called at most once.
	 */
	private boolean isActive(SpanContextInformation spanContext, SpanReporter spanReporter) {
		final String requestAttributeActive = ClassUtils.getIdentityString(spanReporter) + ".active";
		final Boolean activeFromAttribute = (Boolean) spanContext.getRequestAttribute(requestAttributeActive);
		if (activeFromAttribute != null) {
			return activeFromAttribute;
		}
		final boolean active = spanReporter.isActive(SpanContextInformation.of(spanContext.getSpan(), null, spanContext.getRequestAttributes()));
		spanContext.addRequestAttribute(requestAttributeActive, active);
		return active;
	}

	/**
	 * @return the {@link Span} of the current request or a noop {@link Span} (never <code>null</code>)
	 */
	public Span getSpan() {
		final SpanContextInformation spanContext = getSpanContext();
		if (spanContext != null) {
			return spanContext.getSpan() != null ? spanContext.getSpan() : NoopSpan.INSTANCE;
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
		request.remove();
	}

	void setCallTreeMeter(Meter callTreeMeter) {
		this.callTreeMeter = callTreeMeter;
	}

}
