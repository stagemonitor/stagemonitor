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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class RequestMonitor {

	private static final Logger logger = LoggerFactory.getLogger(RequestMonitor.class);
	private final RequestLogger requestLogger = new RequestLogger();

	/**
	 * Helps to detect, if this request is the 'real' one or just the forwarding one.
	 * Example: /a is forwarding the request to /b. /a is the forwarding request /b is the real or forwarded request.
	 * Only /b will be measured, /a will be ignored.
	 * <p/>
	 * To enable this behaviour in a web environment, make sure to include
	 * <code>&lt;dispatcher>FORWARD&lt;/dispatcher></code> in the web.xml filter definition.
	 */
	private static ThreadLocal<String> actualRequestName = new ThreadLocal<String>();
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
			info.executionResult = monitoredRequest.execute();
			return info;
		} catch (Exception e) {
			info.requestTrace.setException(e);
			throw e;
		} finally {
			if (monitor) {
				afterExecution(monitoredRequest, info);
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
		request.set(info.requestTrace);
		try {
			if (info.monitorThisExecution()) {
				if (actualRequestName.get() != null) {
					info.forwardedExecution = true;
					if (!monitoredRequest.isMonitorForwardedExecutions()) {
						info.requestTrace = null;
						return;
					}
				}
				actualRequestName.set(info.requestTrace.getName());
				info.timer = metricRegistry.timer(getTimerMetricName(info.getTimerName()));
				if (info.profileThisExecution()) {
					final CallStackElement root = Profiler.activateProfiling("total");
					info.requestTrace.setCallStack(root);
				}
			}
		} catch (RuntimeException e) {
			logger.warn(e.getMessage() + " (this exception is ignored) actualRequestName=" + actualRequestName.get() +
					info.toString(), e);
		}
	}

	private <T extends RequestTrace> void afterExecution(MonitoredRequest<T> monitoredRequest,
															 RequestInformation<T> info) {
		try {
			if (info.monitorThisExecution()) {
				// if forwarded executions are not monitored, info.requestTrace would be null
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
					String timerMetricName = getTimerMetricName(info.getTimerName());
					if (info.timer.getCount() == 0 && metricRegistry.getMetrics().get(timerMetricName) != null) {
						metricRegistry.remove(timerMetricName);
					}
				}
			}
		} catch (RuntimeException e) {
			logger.warn(e.getMessage() + " (this exception is ignored) actualRequestName=" + actualRequestName.get()
					+ info.toString(), e);
		} finally {
			/*
			 * The forwarded execution is executed in the same thread.
			 * Only remove the thread local on the topmost execution context,
			 * otherwise info.isForwardingExecution() doesn't work
			 */
			if (!info.forwardedExecution) {
				actualRequestName.remove();
			}
			if (info.requestTrace != null) {
				Profiler.clearMethodCallParent();
				request.remove();
			}
		}
	}

	private <T extends RequestTrace> void trackMetrics(RequestInformation<T> info, long executionTime, long cpuTime) {
		if (info.timer != null) {
			info.timer.update(executionTime, NANOSECONDS);
			metricRegistry.timer("request._all.time.server").update(executionTime, NANOSECONDS);
			String timerName = info.getTimerName();
			if (configuration.isCollectCpuTime()) {
				metricRegistry.timer(name("request", timerName, "cpu-time.server")).update(cpuTime, NANOSECONDS);
			}
			T requestTrace = info.requestTrace;
			if (requestTrace.isError()) {
				metricRegistry.meter(name("request", timerName, "meter.error")).mark();
			}
			if (requestTrace.getExecutionCountDb() > 0) {
				metricRegistry.timer(name("request", timerName, "time.db")).update(requestTrace.getExecutionTimeDb(),
						MILLISECONDS);
				metricRegistry.meter(name("request", timerName, "meter.db")).mark(requestTrace.getExecutionCountDb());
			}
		}
	}

	private <T extends RequestTrace> String getTimerMetricName(String timerName) {
		return name("request", timerName, "time.server");
	}

	private <T extends RequestTrace> void reportCallStack(T requestTrace, String serverUrl) {
		if (serverUrl != null && !serverUrl.isEmpty()) {
			final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			String path = String.format("/stagemonitor-%s/executions/%s", dateFormat.format(new Date()),
					requestTrace.getId());
			final String ttl = configuration.getCallStacksTimeToLive();
			if (ttl != null && !ttl.isEmpty()) {
				path += "?ttl=" + ttl;
			}
			RestClient.sendAsJsonAsync(serverUrl, path, "PUT", requestTrace);
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
		private Timer timer = null;
		T requestTrace = null;
		private long start = System.nanoTime();
		private long startCpu = getCpuTime();
		boolean forwardedExecution = false;
		private Object executionResult = null;

		private boolean profileThisExecution() {
			int callStackEveryXRequestsToGroup = configuration.getCallStackEveryXRequestsToGroup();
			if (callStackEveryXRequestsToGroup == 1) return true;
			if (callStackEveryXRequestsToGroup < 1) return false;
			if (timer.getCount() == 0) return false;
			return timer.getCount() % callStackEveryXRequestsToGroup == 0;
		}

		private boolean monitorThisExecution() {
			return requestTrace != null && requestTrace.getName() != null && !requestTrace.getName().isEmpty();
		}

		public String getTimerName() {
			return name(GraphiteSanitizer.sanitizeGraphiteMetricSegment(requestTrace.getName()));
		}

		public T getRequestTrace() {
			return requestTrace;
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
			return !requestTrace.getName().equals(actualRequestName.get());
		}

		public Object getExecutionResult() {
			return executionResult;
		}

		@Override
		public String toString() {
			return "RequestInformation{" +
					"timer=" + timer +
					", requestTrace=" + requestTrace +
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
