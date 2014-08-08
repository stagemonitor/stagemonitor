package org.stagemonitor.requestmonitor;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.StageMonitor;
import org.stagemonitor.core.rest.RestClient;
import org.stagemonitor.core.util.GraphiteSanitizer;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class RequestMonitor {

	private static final Logger logger = LoggerFactory.getLogger(RequestMonitor.class);
	private static final String REQUEST = "request";
	private final RequestLogger requestLogger = new RequestLogger();

	/**
	 * Helps to detect, if this request is the 'real' one or just the forwarding one.
	 * Example: /a is forwarding the request to /b. /a is the forwarding request /b is the real or forwarded request.
	 * Only /b will be measured, /a will be ignored.
	 * <p/>
	 * To enable this behaviour in a web environment, make sure to include
	 * <code>&lt;dispatcher>FORWARD&lt;/dispatcher></code> in the web.xml filter definition.
	 */
	private static ThreadLocal<RequestTrace> request = new ThreadLocal<RequestTrace>();

	private int warmupRequests = 0;
	private AtomicBoolean warmedUp = new AtomicBoolean(false);
	private AtomicInteger noOfRequests = new AtomicInteger(0);
	private MetricRegistry metricRegistry;
	private Configuration configuration;
	private ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

	private Date endOfWarmup;

	public RequestMonitor() {
		this(StageMonitor.getConfiguration());
	}

	public RequestMonitor(Configuration configuration) {
		warmupRequests = configuration.getNoOfWarmupRequests();
		this.metricRegistry = StageMonitor.getMetricRegistry();
		this.configuration = configuration;
		endOfWarmup = new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(configuration.getWarmupSeconds()));
	}

	public void setMeasurementSession(MeasurementSession measurementSession) {
		StageMonitor.startMonitoring(measurementSession);
	}

	public <T extends RequestTrace> RequestInformation<T> monitor(MonitoredRequest<T> monitoredRequest) throws Exception {
		long overhead1 = System.nanoTime();
		if (!configuration.isStagemonitorActive()) {
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
		final boolean monitor = configuration.isCollectRequestStats() && isWarmedUp();
		if (monitor) {
			beforeExecution(monitoredRequest, info);
		}
		try {
			overhead1 = System.nanoTime() - overhead1;
			info.executionResult = monitoredRequest.execute();
			return info;
		} catch (Exception e) {
			info.requestTrace.setException(e);
			throw e;
		} finally {
			long overhead2 = System.nanoTime();
			if (monitor) {
				afterExecution(monitoredRequest, info);
			}

			if (configuration.isInternalMonitoringActive()) {
				overhead2 = System.nanoTime() - overhead2;
				logger.info("overhead1="+overhead1);
				logger.info("overhead2="+overhead2);
				metricRegistry.timer("internal.overhead.RequestMonitor")
						.update(overhead2 + overhead1, NANOSECONDS);
			}
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
			MeasurementSession session = new MeasurementSession(configuration.getApplicationName(), getHostName(),
					configuration.getInstanceName());
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
		try {
			if (info.monitorThisExecution()) {
				// if forwarded executions are not monitored, info.monitorThisExecution() would have returned false
				if (!info.isForwardingExecution()) {
					final long executionTime = System.nanoTime() - info.start;
					final long cpuTime = getCpuTime() - info.startCpu;
					info.requestTrace.setExecutionTime(NANOSECONDS.toMillis(executionTime));
					info.requestTrace.setExecutionTimeCpu(NANOSECONDS.toMillis(cpuTime));
					monitoredRequest.onPostExecute(info);

					if (info.requestTrace.getCallStack() != null) {
						Profiler.stop();
						reportCallStack(info.requestTrace, configuration.getElasticsearchUrl());
					}
					trackMetrics(info, executionTime, cpuTime);
				} else {
					if (info.timerCreated) {
						String timerMetricName = getTimerMetricName(info.getTimerName());
						if (info.getRequestTimer().getCount() == 0 && metricRegistry.getMetrics().get(timerMetricName) != null) {
							metricRegistry.remove(timerMetricName);
						}
					}
				}
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
			if (info.requestTrace != null) {
				Profiler.clearMethodCallParent();
			}
		}
	}

	private <T extends RequestTrace> void trackMetrics(RequestInformation<T> info, long executionTime, long cpuTime) {
		T requestTrace = info.requestTrace;
		String timerName = info.getTimerName();

		info.getRequestTimer().update(executionTime, NANOSECONDS);
		metricRegistry.timer(getTimerMetricName("All")).update(executionTime, NANOSECONDS);

		if (configuration.isCollectCpuTime()) {
			metricRegistry.timer(name(REQUEST, timerName, ".server.cpu-time.total")).update(cpuTime, NANOSECONDS);
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
			if (configuration.collectDbTimePerRequest()) {
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

	private <T extends RequestTrace> void reportCallStack(T requestTrace, String serverUrl) {
		if (serverUrl != null && !serverUrl.isEmpty()) {
			RestClient.sendCallStackAsync(requestTrace, requestTrace.getId(), serverUrl);
		}
		if (configuration.isLogCallStacks()) {
			requestLogger.logStats(requestTrace);
		}
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
		return threadMXBean.isCurrentThreadCpuTimeSupported() ? threadMXBean.getCurrentThreadCpuTime() : 0L;
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
			int callStackEveryXRequestsToGroup = configuration.getCallStackEveryXRequestsToGroup();
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

	public static RequestTrace getRequest() {
		return request.get();
	}

}
