package org.stagemonitor.requestmonitor;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.uber.jaeger.context.TracingUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.core.util.CompletedFuture;
import org.stagemonitor.core.util.ExecutorUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.core.util.TimeUtils;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;
import org.stagemonitor.requestmonitor.reporter.SpanReporter;
import org.stagemonitor.requestmonitor.utils.IPAnonymizationUtils;
import org.stagemonitor.requestmonitor.utils.SpanTags;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.opentracing.NoopTracer;
import io.opentracing.Span;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;
import static org.stagemonitor.requestmonitor.reporter.ServerRequestMetricsReporter.getTimerMetricName;

public class RequestMonitor {

	private static final Logger logger = LoggerFactory.getLogger(RequestMonitor.class);

	/**
	 * Helps to detect, if this request is the 'real' one or just the forwarding one. Example: /a is forwarding the
	 * request to /b. /a is the forwarding request /b is the real or forwarded request. Only /b will be measured, /a
	 * will be ignored.
	 * <p/>
	 * To enable this behaviour in a web environment, make sure to set stagemonitor.web.monitorOnlyForwardedRequests to
	 * true.
	 */
	// TODO remove static keyword. This is currently needed for tests
	private static ThreadLocal<RequestInformation<? extends RequestTrace>> request = new ThreadLocal<RequestInformation<? extends RequestTrace>>();

	private final List<SpanReporter> spanReporters = new CopyOnWriteArrayList<SpanReporter>();

	private final List<Runnable> onBeforeRequestCallbacks = new CopyOnWriteArrayList<Runnable>();

	private final List<Runnable> onAfterRequestCallbacks = new CopyOnWriteArrayList<Runnable>();

	private final MetricName internalOverheadMetricName = name("internal_overhead_request_monitor").build();

	private ExecutorService asyncRequestTraceReporterPool;

	private int warmupRequests = 0;
	private AtomicBoolean warmedUp = new AtomicBoolean(false);
	private AtomicInteger noOfRequests = new AtomicInteger(0);
	private final Configuration configuration;
	private Metric2Registry metricRegistry;
	private CorePlugin corePlugin;
	private RequestMonitorPlugin requestMonitorPlugin;
	private Date endOfWarmup;
	private Meter callTreeMeter = new Meter();
	private final static Span NOOP_SPAN = new NoopTracer().buildSpan(null).start();

	public RequestMonitor(Configuration configuration, Metric2Registry registry) {
		this(configuration, registry, ServiceLoader.load(SpanReporter.class, RequestMonitor.class.getClassLoader()));
	}

	public RequestMonitor(Configuration configuration, Metric2Registry registry, Iterable<SpanReporter> requestTraceReporters) {
		this(configuration, registry, configuration.getConfig(RequestMonitorPlugin.class), requestTraceReporters);
	}

	private RequestMonitor(Configuration configuration, Metric2Registry registry, RequestMonitorPlugin requestMonitorPlugin,
						   Iterable<SpanReporter> requestTraceReporters) {
		this.configuration = configuration;
		this.metricRegistry = registry;
		this.corePlugin = configuration.getConfig(CorePlugin.class);
		this.requestMonitorPlugin = requestMonitorPlugin;
		this.warmupRequests = requestMonitorPlugin.getNoOfWarmupRequests();
		this.endOfWarmup = new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(requestMonitorPlugin.getWarmupSeconds()));
		this.asyncRequestTraceReporterPool = ExecutorUtils
				.createSingleThreadDeamonPool("async-request-reporter", corePlugin.getThreadPoolQueueCapacityLimit());
		for (SpanReporter spanReporter : requestTraceReporters) {
			addReporter(spanReporter);
		}
	}

	public <T extends RequestTrace> void monitorStart(MonitoredRequest<T> monitoredRequest) {
		final long start = System.nanoTime();
		RequestInformation<T> info = new RequestInformation<T>();
		info.monitoredRequest = monitoredRequest;
		detectForwardedRequest(info);
		request.set(info);
		try {
			if (!corePlugin.isStagemonitorActive()) {
				return;
			}

			if (Stagemonitor.getMeasurementSession().isNull()) {
				createMeasurementSession();
			}

			info.firstRequest = noOfRequests.get() == 0;
			if (Stagemonitor.getMeasurementSession().getInstanceName() == null) {
				getInstanceNameFromExecution(monitoredRequest);
			}

			if (!Stagemonitor.isStarted()) {
				Stagemonitor.startMonitoring();
			}
			if (info.monitorThisRequest()) {
				beforeExecution(monitoredRequest, info);
			}
		} finally {
			info.overhead1 = System.nanoTime() - start;
		}
	}

	public <T extends RequestTrace> void monitorStop() {
		long overhead2 = System.nanoTime();
		final RequestInformation<T> info = (RequestInformation<T>) request.get();
		request.set(info.parent);
		if (info.getSpan() != null) {
			TracingUtils.getTraceContext().pop();
		}
		if (info.monitorThisRequest() && info.hasRequestName()) {
			try {
				monitorAfterExecution(info.monitoredRequest, info);
			} catch (Exception e) {
				logger.warn(e.getMessage() + " (this exception is ignored) " + info.toString(), e);
			}
		} else {
			removeTimerIfCountIsZero(info);
		}

		cleanUpAfter(info);

		if (!info.firstRequest) {
			trackOverhead(info.overhead1, overhead2);
		}

		for (Runnable onAfterRequestCallback : onAfterRequestCallbacks) {
			try {
				onAfterRequestCallback.run();
			} catch (RuntimeException e) {
				logger.warn(e.getMessage() + " (this exception is ignored) " + info.toString(), e);
			}
		}
	}

	private <T extends RequestTrace> void cleanUpAfter(RequestInformation<T> info) {
		if (info.requestTrace != null) {
			Profiler.clearMethodCallParent();
		}
	}

	public <T extends RequestTrace> RequestInformation<T> monitor(MonitoredRequest<T> monitoredRequest) throws Exception {
		try {
			monitorStart(monitoredRequest);
			final RequestInformation<T> info = (RequestInformation<T>) request.get();
			info.executionResult = monitoredRequest.execute();
			return info;
		} catch (Exception e) {
			recordException(e);
			throw e;
		} finally {
			monitorStop();
		}
	}

	public void recordException(Exception e) {
		SpanTags.setException(getSpan(), e, requestMonitorPlugin.getIgnoreExceptions(), requestMonitorPlugin.getUnnestExceptions());
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
	private synchronized void getInstanceNameFromExecution(MonitoredRequest<?> monitoredRequest) {
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

	private <T extends RequestTrace> void beforeExecution(MonitoredRequest<T> monitoredRequest, RequestInformation<T> info) {
		info.requestTrace = monitoredRequest.createRequestTrace();
		info.span = monitoredRequest.createSpan();
		for (Map.Entry<String, String> entry : Stagemonitor.getMeasurementSession().asMap().entrySet()) {
			info.span.setTag(entry.getKey(), entry.getValue());
		}
		if (info.requestTrace != null) {
			info.requestTrace.setSpan(info.span);
		}
		TracingUtils.getTraceContext().push(info.span);

		try {
			if (info.isProfileThisRequest()) {
				callTreeMeter.mark();
				final CallStackElement root = Profiler.activateProfiling("total");
				info.callTree = root;
			}
		} catch (RuntimeException e) {
			logger.warn(e.getMessage() + " (this exception is ignored) " + info.toString(), e);
		}
		for (Runnable onBeforeRequestCallback : onBeforeRequestCallbacks) {
			try {
				onBeforeRequestCallback.run();
			} catch (RuntimeException e) {
				logger.warn(e.getMessage() + " (this exception is ignored) " + info.toString(), e);
			}
		}
	}

	private <T extends RequestTrace> void detectForwardedRequest(RequestInformation<T> info) {
		if (request.get() != null) {
			// there is already an request set in this thread -> this execution must have been forwarded
			info.parent = (RequestInformation<T>) request.get();
			info.parent.child = info;
		}
	}

	private <T extends RequestTrace> void monitorAfterExecution(MonitoredRequest<T> monitoredRequest, RequestInformation<T> info) {
		final T requestTrace = info.requestTrace;
		final Span span = info.span;
		final long cpuTime = TimeUtils.getCpuTime() - info.startCpu;
		if (requestTrace != null) {
			span.setTag("duration_cpu", NANOSECONDS.toMicros(cpuTime));
			span.setTag("duration_cpu_ms", NANOSECONDS.toMillis(cpuTime));
			monitoredRequest.onPostExecute(info);
			anonymizeUserNameAndIp(requestTrace);

			final CallStackElement callTree = info.getCallTree();
			if (callTree != null) {
				Profiler.stop();
				callTree.setSignature(requestTrace.getName());
				final double minExecutionTimeMultiplier = requestMonitorPlugin.getMinExecutionTimePercent() / 100;
				if (minExecutionTimeMultiplier > 0d) {
					callTree.removeCallsFasterThan((long) (callTree.getExecutionTime() * minExecutionTimeMultiplier));
				}
				SpanTags.setCallTree(span, callTree);
			}
		}
		span.finish();
		info.requestTraceReporterFuture = report(info);
	}

	void anonymizeUserNameAndIp(RequestTrace requestTrace) {
		final String username = requestTrace.getUsername();
		if (requestMonitorPlugin.isPseudonymizeUserNames()) {
			requestTrace.setUsername(StringUtils.sha1Hash(username));
		}
		final boolean disclose = requestMonitorPlugin.getDiscloseUsers().contains(requestTrace.getUsername());
		if (disclose) {
			requestTrace.setDisclosedUserName(username);
		}
		if (requestTrace.getClientIp() != null && requestMonitorPlugin.isAnonymizeIPs() && !disclose) {
			requestTrace.setClientIp(IPAnonymizationUtils.anonymize(requestTrace.getClientIp()));
		}
	}

	private <T extends RequestTrace> void removeTimerIfCountIsZero(RequestInformation<T> info) {
		final String requestName = info.getRequestName();
		if (requestName != null) {
			MetricName timerMetricName = getTimerMetricName(requestName);
			final Timer timer = metricRegistry.getTimers().get(timerMetricName);
			if (timer != null && timer.getCount() == 0) {
				metricRegistry.remove(timerMetricName);
			}
		}
	}

	private <T extends RequestTrace> Future<?> report(final RequestInformation<T> requestInformation) {
		try {
			if (requestMonitorPlugin.isReportAsync()) {
				return asyncRequestTraceReporterPool.submit(new Runnable() {
					@Override
					public void run() {
						doReport(requestInformation);
					}
				});
			} else {
				doReport(requestInformation);
				return new CompletedFuture<Object>(null);
			}
		} catch (RejectedExecutionException e) {
			ExecutorUtils.logRejectionWarning(e);
			return new CompletedFuture<Object>(null);
		}
	}

	private <T extends RequestTrace> void doReport(RequestInformation<T> requestInformation) {
		for (SpanReporter spanReporter : spanReporters) {
			final CallStackElement callTree = requestInformation.getCallTree();
			if (isActive(requestInformation, spanReporter)) {
				try {
					spanReporter.report(new SpanReporter.ReportArguments(requestInformation.getRequestTrace(),
							requestInformation.getSpan(), callTree, requestInformation.requestAttributes));
				} catch (Exception e) {
					logger.warn(e.getMessage() + " (this exception is ignored)", e);
				}
			}
			if (callTree != null) {
				callTree.recycle();
			}
		}
	}

	public boolean isWarmedUp() {
		if (!warmedUp.get()) {
			warmedUp.set(warmupRequests < noOfRequests.incrementAndGet() && new Date(System.currentTimeMillis() + 1).after(endOfWarmup));
			return warmedUp.get();
		} else {
			return true;
		}
	}

	void onInit(StagemonitorPlugin.InitArguments initArguments) {
		for (SpanReporter spanReporter : spanReporters) {
			spanReporter.init(new SpanReporter.InitArguments(configuration, initArguments.getMetricRegistry()));
		}
	}

	public class RequestInformation<T extends RequestTrace> {
		private T requestTrace = null;
		private Span span;
		private long startCpu = TimeUtils.getCpuTime();
		private Object executionResult = null;
		private long overhead1;
		private MonitoredRequest monitoredRequest;
		private boolean firstRequest;
		private RequestInformation<T> parent;
		private RequestInformation<T> child;
		private Future<?> requestTraceReporterFuture;
		private Map<String, Object> requestAttributes = new HashMap<String, Object>();
		private CallStackElement callTree;

		/**
		 * If the request has no name it means that it should not be monitored.
		 *
		 * @return <code>true</code>, if the request trace has a name, <code>false</code> otherwise
		 */
		private boolean hasRequestName() {
			final String requestName = getRequestName();
			return requestName != null && !requestName.isEmpty();
		}

		private boolean monitorThisRequest() {
			final String msg = "This reqest is not monitored because {}";
			if (!Stagemonitor.isStarted()) {
				logger.debug(msg, "stagemonitor has not been started yet");
				return false;
			}
			if (!requestMonitorPlugin.isCollectRequestStats()) {
				logger.debug(msg, "the collection of request stats is disabled");
				return false;
			}
			if (!isWarmedUp()) {
				logger.debug(msg, "the application is not warmed up");
				return false;
			}

			if (isForwarded() && isForwarding()) {
				logger.debug(msg, "this request is both forwarded and forwarding");
				return false;
			}
			if (isForwarded()) {
				if (monitoredRequest.isMonitorForwardedExecutions()) {
					return true;
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug(msg, "this is a forwarded request and monitoring forwarded requests is disabled for " + monitoredRequest.getClass().getSimpleName());
					}
					return false;
				}
			}
			if (isForwarding()) {
				if (!monitoredRequest.isMonitorForwardedExecutions()) {
					return true;
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug(msg, "this is a forwarding request and monitoring forwarding requests is disabled for " + monitoredRequest.getClass().getSimpleName());
					}
					return false;
				}
			}
			return true;
		}

		public String getRequestName() {
			if (span instanceof com.uber.jaeger.Span) {
				return ((com.uber.jaeger.Span) span).getOperationName();
			}
			return null;
		}

		/**
		 * Returns the request trace or <code>null</code>, if
		 * {@link org.stagemonitor.requestmonitor.RequestMonitor#isWarmedUp()} is <code>false</code>
		 *
		 * @return the request trace or <code>null</code>, if {@link org.stagemonitor.requestmonitor.RequestMonitor#isWarmedUp()}
		 * is <code>false</code>
		 */
		public T getRequestTrace() {
			return requestTrace;
		}

		private boolean isProfileThisRequest() {
			if (span == null || getInternalSpan().isRPCClient()) {
				return false;
			}
			double callTreeRateLimit = requestMonitorPlugin.getOnlyCollectNCallTreesPerMinute();
			if (!requestMonitorPlugin.isProfilerActive()) {
				logger.debug("Not profiling this request because stagemonitor.profiler.active=false");
				return false;
			} else if (callTreeRateLimit <= 0) {
				logger.debug("Not profiling this request because stagemonitor.requestmonitor.onlyReportNRequestsPerMinuteToElasticsearch <= 0");
				return false;
			} else if (!isAnyRequestTraceReporterActiveWhichNeedsTheCallTree(this)) {
				logger.debug("Not profiling this request because no RequestTraceReporter is active {}", spanReporters);
				return false;
			} else if (callTreeRateLimit < 1000000d && callTreeMeter.getOneMinuteRate() >= callTreeRateLimit) {
				logger.debug("Not profiling this request because more than {} call trees per minute where created", callTreeRateLimit);
				return false;
			}
			return true;
		}

		/**
		 * A forwarding request is one that forwards a request to another execution.
		 * <p/>
		 * Examples:
		 * <p/>
		 * - web environment: request to /a makes a
		 * {@link javax.servlet.RequestDispatcher#forward(ServletRequest, ServletResponse)} to /b.
		 * /a is the forwarding execution, /b is the forwarded execution
		 * <p/>
		 * - plain method calls: monitored method a() calls monitored method b()
		 * (monitored means monitored by {@link RequestMonitor}).
		 * Method a() is the forwarding execution, Method b() is the forwarded execution.
		 *
		 * @return true, if this request is a forwarding request, false otherwise
		 */
		private boolean isForwarding() {
			return child != null;
		}

		public Object getExecutionResult() {
			return executionResult;
		}

		@Override
		public String toString() {
			return "RequestInformation{" +
					"requestTrace=" + requestTrace +
					", startCpu=" + startCpu +
					", forwardedExecution=" + isForwarded() +
					", executionResult=" + executionResult +
					'}';
		}

		public boolean isForwarded() {
			return parent != null;
		}

		public Future<?> getRequestTraceReporterFuture() {
			return requestTraceReporterFuture;
		}

		public Span getSpan() {
			return span;
		}

		public com.uber.jaeger.Span getInternalSpan() {
			return SpanTags.getInternalSpan(span);
		}

		public void setSpan(Span span) {
			this.span = span;
		}

		/**
		 * Adds an attribute to the request which can later be retrieved by {@link #getRequestAttribute(String)}
		 * <p/>
		 * The attributes won't be reported
		 */
		public void addRequestAttribute(String key, Object value) {
			requestAttributes.put(key, value);
		}

		public Object getRequestAttribute(String key) {
			return requestAttributes.get(key);
		}

		public CallStackElement getCallTree() {
			return callTree;
		}
	}

	private boolean isAnyRequestTraceReporterActiveWhichNeedsTheCallTree(RequestInformation<?> requestInformation) {
		for (SpanReporter reporter : spanReporters) {
			if (reporter.requiresCallTree() && isActive(requestInformation, reporter)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether the given {@link SpanReporter} is active for the current {@link RequestTrace}. If this method was
	 * already called for a {@link SpanReporter} in the context of the current request it returns the previous result.
	 * In other words this method makes sure that {@link SpanReporter#isActive(SpanReporter.IsActiveArguments)} is
	 * called at most once.
	 */
	private boolean isActive(RequestInformation<?> requestInformation, SpanReporter spanReporter) {
		final String requestAttributeActive = ClassUtils.getIdentityString(spanReporter) + ".active";
		final Boolean activeFromAttribute = (Boolean) requestInformation.getRequestAttribute(requestAttributeActive);
		if (activeFromAttribute != null) {
			return activeFromAttribute;
		}
		final boolean active = spanReporter.isActive(new SpanReporter.IsActiveArguments(requestInformation.requestTrace, requestInformation.span, requestInformation.requestAttributes));
		requestInformation.addRequestAttribute(requestAttributeActive, active);
		return active;
	}

	/**
	 * @return the {@link RequestTrace} of the current request
	 * @deprecated use <code>RequestMonitor.get().getRequestTrace()</code> or <code>RequestMonitor.get().ifRequestPresent(RequestTraceConsumer)</code>
	 */
	@Deprecated
	public static RequestTrace getRequest() {
		final RequestInformation<? extends RequestTrace> requestInformation = RequestMonitor.get().request.get();
		return requestInformation != null ? requestInformation.getRequestTrace() : null;
	}

	/**
	 * @return the {@link RequestTrace} of the current request (may be <code>null</code>)
	 */
	public RequestTrace getRequestTrace() {
		final RequestInformation<? extends RequestTrace> requestInformation = request.get();
		return requestInformation != null ? requestInformation.getRequestTrace() : null;
	}

	/**
	 * @return the {@link Span} of the current request or a noop {@link Span} (never <code>null</code>)
	 */
	public Span getSpan() {
		final RequestInformation<? extends RequestTrace> requestInformation = request.get();
		return requestInformation != null ? requestInformation.getSpan() : NOOP_SPAN;
	}

	/**
	 * Adds a {@link SpanReporter}
	 *
	 * @param spanReporter the {@link SpanReporter} to add
	 */
	public static void addRequestTraceReporter(SpanReporter spanReporter) {
		get().addReporter(spanReporter);
	}

	/**
	 * Gets the {@link RequestMonitor}.
	 * <p/>
	 * You can use this instance for example to call the following methods:
	 * <ul>
	 * <li>{@link #addReporter(SpanReporter)}</li>
	 * <li>{@link #addOnBeforeRequestCallback(Runnable)}</li>
	 * <li>{@link #addOnAfterRequestCallback(Runnable)}</li>
	 * <li>{@link #getRequestTrace()}</li>
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

	public <T extends SpanReporter> T getReporter(Class<T> reporterClass) {
		for (SpanReporter spanReporter : spanReporters) {
			if (spanReporter.getClass() == reporterClass) {
				return (T) spanReporter;
			}
		}
		return null;
	}

	/**
	 * Shuts down the internal thread pool
	 */
	public void close() {
		asyncRequestTraceReporterPool.shutdown();
		request.remove();
	}

	/**
	 * Registers a callback that is called before a request gets executed
	 * <p/>
	 * This is a internal method. The API is likely to change!
	 *
	 * @param onBeforeRequestCallback The callback
	 */
	public void addOnBeforeRequestCallback(Runnable onBeforeRequestCallback) {
		onBeforeRequestCallbacks.add(onBeforeRequestCallback);
	}

	/**
	 * Registers a callback that is called after a request has been executed
	 * <p/>
	 * This is a internal method. The API is likely to change!
	 *
	 * @param onAfterRequestCallback The callback
	 */
	public void addOnAfterRequestCallback(Runnable onAfterRequestCallback) {
		onAfterRequestCallbacks.add(onAfterRequestCallback);
	}

	void setCallTreeMeter(Meter callTreeMeter) {
		this.callTreeMeter = callTreeMeter;
	}

}
