package org.stagemonitor.requestmonitor;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.StageMonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.rest.RestClient;
import org.stagemonitor.core.util.GraphiteSanitizer;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class RequestMonitor {

	private static final Logger logger = LoggerFactory.getLogger(RequestMonitor.class);
	private static final String REQUEST = "request";

	/**
	 * Helps to detect, if this request is the 'real' one or just the forwarding one.
	 * Example: /a is forwarding the request to /b. /a is the forwarding request /b is the real or forwarded request.
	 * Only /b will be measured, /a will be ignored.
	 * <p/>
	 * To enable this behaviour in a web environment, make sure to include
	 * <code>&lt;dispatcher>FORWARD&lt;/dispatcher></code> in the web.xml filter definition.
	 */
	private static ThreadLocal<RequestTrace> request = new ThreadLocal<RequestTrace>();

	private static final List<RequestTraceReporter> requestTraceReporters = new CopyOnWriteArrayList<RequestTraceReporter>(){{
		add(new ElasticsearchRequestTraceReporter());
		add(new LogRequestTraceReporter());
	}};

	private static ExecutorService asyncRequestTraceReporterPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			thread.setName("async-request-reporter");
			return thread;
		}
	});

	private int warmupRequests = 0;
	private AtomicBoolean warmedUp = new AtomicBoolean(false);
	private AtomicInteger noOfRequests = new AtomicInteger(0);
	private MetricRegistry metricRegistry;
	private CorePlugin corePlugin;
	private RequestMonitorPlugin requestMonitorPlugin;
	private ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	private final boolean isCurrentThreadCpuTimeSupported = threadMXBean.isCurrentThreadCpuTimeSupported();

	private Date endOfWarmup;

	public RequestMonitor() {
		this(StageMonitor.getConfiguration());
	}

	public RequestMonitor(Configuration configuration) {
		this(configuration, StageMonitor.getMetricRegistry());
	}

	public RequestMonitor(Configuration configuration, MetricRegistry registry) {
		this(configuration.getConfig(CorePlugin.class), registry, configuration.getConfig(RequestMonitorPlugin.class));
	}

	public RequestMonitor(CorePlugin corePlugin, MetricRegistry registry, RequestMonitorPlugin requestMonitorPlugin) {
		this.metricRegistry = registry;
		this.corePlugin = corePlugin;
		this.requestMonitorPlugin = requestMonitorPlugin;
		warmupRequests = requestMonitorPlugin.getNoOfWarmupRequests();
		endOfWarmup = new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(requestMonitorPlugin.getWarmupSeconds()));
	}

	public void setMeasurementSession(MeasurementSession measurementSession) {
		StageMonitor.startMonitoring(measurementSession);
	}

	public <T extends RequestTrace> RequestInformation<T> monitor(MonitoredRequest<T> monitoredRequest) throws Exception {
		long overhead1 = System.nanoTime();
		if (!corePlugin.isStagemonitorActive()) {
			RequestInformation<T> info = new RequestInformation<T>();
			info.executionResult = monitoredRequest.execute();
			return info;
		}

		if (StageMonitor.getMeasurementSession().isNull()) {
			createMeasurementSession();
		}

		if (StageMonitor.getMeasurementSession().getInstanceName() == null && noOfRequests.get() == 0) {
			getInstanceNameFromExecution(monitoredRequest);
		}

		RequestInformation<T> info = new RequestInformation<T>();
		final boolean monitor = requestMonitorPlugin.isCollectRequestStats() && isWarmedUp();
		if (monitor) {
			beforeExecution(monitoredRequest, info);
		}
		try {
			overhead1 = System.nanoTime() - overhead1;
			info.executionResult = monitoredRequest.execute();
			return info;
		} catch (Exception e) {
			recordException(info, e);
			throw e;
		} finally {
			long overhead2 = System.nanoTime();
			if (monitor) {
				afterExecution(monitoredRequest, info);
			}

			trackOverhead(overhead1, overhead2);
		}
	}

	private void recordException(RequestInformation<?> info, Exception e) throws Exception {
		if (info.requestTrace != null) {
			info.requestTrace.setException(e);
		}
	}

	private void trackOverhead(long overhead1, long overhead2) {
		if (corePlugin.isInternalMonitoringActive()) {
			overhead2 = System.nanoTime() - overhead2;
			metricRegistry.timer("internal.overhead.RequestMonitor").update(overhead2 + overhead1, NANOSECONDS);
		}
	}

	/*
	 * In case the instance name is not set by configuration, try to read from monitored execution
	 * (e.g. the domain name from a HTTP request)
	 */
	private synchronized void getInstanceNameFromExecution(MonitoredRequest<?> monitoredRequest) {
		final MeasurementSession measurementSession = StageMonitor.getMeasurementSession();
		if (measurementSession.getInstanceName() == null) {
			MeasurementSession session = new MeasurementSession(measurementSession.getApplicationName(), measurementSession.getHostName(),
					monitoredRequest.getInstanceName());
			StageMonitor.startMonitoring(session);
		}
	}

	private synchronized void createMeasurementSession() {
		if (StageMonitor.getMeasurementSession().isNull()) {
			MeasurementSession session = new MeasurementSession(corePlugin.getApplicationName(), getHostName(),
					corePlugin.getInstanceName());
			setMeasurementSession(session);
		}
	}

	private <T extends RequestTrace> void beforeExecution(MonitoredRequest<T> monitoredRequest, RequestInformation<T> info) {
		info.requestTrace = monitoredRequest.createRequestTrace();
		try {
			detectForwardedRequest(monitoredRequest, info);
			if (!info.monitorThisExecution) {
				return;
			}
			request.set(info.requestTrace);

			if (info.profileThisExecution()) {
				final CallStackElement root = Profiler.activateProfiling("total");
				info.requestTrace.setCallStack(root);
			}
		} catch (RuntimeException e) {
			logger.warn(e.getMessage() + " (this exception is ignored) " + info.toString(), e);
		}
	}

	private <T extends RequestTrace> void detectForwardedRequest(MonitoredRequest<T> monitoredRequest, RequestInformation<T> info) {
		if (request.get() != null) {
			// there is already an request set in this thread -> this execution must have been forwarded
			info.forwardedExecution = true;
			if (!monitoredRequest.isMonitorForwardedExecutions()) {
				info.monitorThisExecution = false;
			}
		}
	}

	private <T extends RequestTrace> void afterExecution(MonitoredRequest<T> monitoredRequest,
															 RequestInformation<T> info) {
		final T requestTrace = info.requestTrace;
		try {
			if (info.monitorThisExecution() && requestTrace.getName() != null && !requestTrace.getName().isEmpty()) {
				monitorAfterExecution(monitoredRequest, info);
			}
		} catch (RuntimeException e) {
			logger.warn(e.getMessage() + " (this exception is ignored) " + info.toString(), e);
		} finally {
			/*
			 * The forwarded execution is executed in the same thread.
			 * Only remove the thread local on the topmost execution context,
			 * otherwise info.isForwardingExecution() doesn't work
			 */
			if (!info.forwardedExecution) {
				request.remove();
			}
			if (requestTrace != null) {
				Profiler.clearMethodCallParent();
			}
		}
	}

	private <T extends RequestTrace> void monitorAfterExecution(MonitoredRequest<T> monitoredRequest, RequestInformation<T> info) {
		final T requestTrace = info.requestTrace;
		// if forwarded executions are not monitored, info.monitorThisExecution() would have returned false
		if (!info.isForwardingExecution()) {
			final long executionTime = System.nanoTime() - info.start;
			final long cpuTime = getCpuTime() - info.startCpu;
			requestTrace.setExecutionTime(NANOSECONDS.toMillis(executionTime));
			requestTrace.setExecutionTimeCpu(NANOSECONDS.toMillis(cpuTime));
			monitoredRequest.onPostExecute(info);

			if (requestTrace.getCallStack() != null) {
				Profiler.stop();
				requestTrace.getCallStack().setSignature(requestTrace.getName());
				reportRequestTrace(requestTrace);
			}
			trackMetrics(info, executionTime, cpuTime);
		} else {
			removeTimerIfCountIsZero(info);
		}
	}

	private <T extends RequestTrace> void removeTimerIfCountIsZero(RequestInformation<T> info) {
		if (info.timerCreated) {
			String timerMetricName = getTimerMetricName(info.getTimerName());
			if (info.getRequestTimer().getCount() == 0 && metricRegistry.getMetrics().get(timerMetricName) != null) {
				metricRegistry.remove(timerMetricName);
			}
		}
	}

	private <T extends RequestTrace> void trackMetrics(RequestInformation<T> info, long executionTime, long cpuTime) {
		T requestTrace = info.requestTrace;
		String timerName = info.getTimerName();

		info.getRequestTimer().update(executionTime, NANOSECONDS);
		metricRegistry.timer(getTimerMetricName("All")).update(executionTime, NANOSECONDS);

		if (requestMonitorPlugin.isCollectCpuTime()) {
			metricRegistry.timer(name(REQUEST, timerName, "server.cpu-time.total")).update(cpuTime, NANOSECONDS);
			metricRegistry.timer("request.All.server.cpu-time.total").update(cpuTime, NANOSECONDS);
		}

		if (requestTrace.isError()) {
			metricRegistry.meter(name(REQUEST, timerName, "server.meter.error")).mark();
			metricRegistry.meter("request.All.server.meter.error").mark();
		}
		trackDbMetrics(timerName, requestTrace);
	}

	private <T extends RequestTrace> void trackDbMetrics(String timerName, T requestTrace) {
		if (requestTrace.getExecutionCountDb() > 0) {
			if (requestMonitorPlugin.isCollectDbTimePerRequest()) {
				metricRegistry.timer(name(REQUEST, timerName, "server.time.db")).update(requestTrace.getExecutionTimeDb(), MILLISECONDS);
				metricRegistry.timer(name("request.All.server.time.db")).update(requestTrace.getExecutionTimeDb(), MILLISECONDS);
			}

			metricRegistry.meter(name(REQUEST, timerName, "server.meter.db")).mark(requestTrace.getExecutionCountDb());
			metricRegistry.meter("request.All.server.meter.db").mark(requestTrace.getExecutionCountDb());
		}
	}

	private <T extends RequestTrace> String getTimerMetricName(String timerName) {
		return name(REQUEST, timerName, "server.time.total");
	}

	private <T extends RequestTrace> void reportRequestTrace(final T requestTrace) {
		asyncRequestTraceReporterPool.submit(new Runnable() {
			@Override
			public void run() {
				for (RequestTraceReporter requestTraceReporter : requestTraceReporters) {
					if (requestTraceReporter.isActive()) {
						requestTraceReporter.reportRequestTrace(requestTrace);
					}
				}
			}
		});
	}

	private boolean isWarmedUp() {
		if (!warmedUp.get()) {
			warmedUp.set(warmupRequests < noOfRequests.incrementAndGet() && new Date(System.currentTimeMillis() + 1).after(endOfWarmup));
			return warmedUp.get();
		} else {
			return true;
		}
	}

	public static String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			logger.warn("Could not get host name. (this exception is ignored)", e);
			return null;
		}
	}

	private long getCpuTime() {
		return isCurrentThreadCpuTimeSupported ? threadMXBean.getCurrentThreadCpuTime() : 0L;
	}

	public class RequestInformation<T extends RequestTrace> {
		private boolean timerCreated = false;
		T requestTrace = null;
		private long start = System.nanoTime();
		private long startCpu = getCpuTime();
		boolean forwardedExecution = false;
		private Object executionResult = null;
		private boolean monitorThisExecution = true;

		boolean monitorThisExecution() {
			return monitorThisExecution;
		}

		public String getTimerName() {
			return name(GraphiteSanitizer.sanitizeGraphiteMetricSegment(requestTrace.getName()));
		}

		public T getRequestTrace() {
			return requestTrace;
		}

		public Timer getRequestTimer() {
			timerCreated = true;
			return metricRegistry.timer(getTimerMetricName(getTimerName()));
		}

		private boolean profileThisExecution() {
			int callStackEveryXRequestsToGroup = requestMonitorPlugin.getCallStackEveryXRequestsToGroup();
			if (callStackEveryXRequestsToGroup == 1) {
				return true;
			}
			if (callStackEveryXRequestsToGroup < 1) {
				return false;
			}
			Timer requestTimer = getRequestTimer();
			if (requestTimer.getCount() == 0) {
				return false;
			}
			return requestTimer.getCount() % callStackEveryXRequestsToGroup == 0;
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
		private boolean isForwardingExecution() {
			return !requestTrace.getId().equals(request.get().getId());
		}

		public Object getExecutionResult() {
			return executionResult;
		}

		@Override
		public String toString() {
			return "RequestInformation{" +
					"requestTrace=" + requestTrace +
					", start=" + start +
					", startCpu=" + startCpu +
					", forwardedExecution=" + forwardedExecution +
					", executionResult=" + executionResult +
					'}';
		}
	}

	/**
	 * @return the {@link RequestTrace} of the current request
	 */
	public static RequestTrace getRequest() {
		return request.get();
	}

	/**
	 * Adds a {@link RequestTraceReporter}
	 *
	 * @param requestTraceReporter the {@link RequestTraceReporter} to add
	 */
	public static void addRequestTraceReporter(RequestTraceReporter requestTraceReporter) {
		requestTraceReporters.add(requestTraceReporter);
	}

}
