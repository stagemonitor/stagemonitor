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

	private int warmupRequests = 0;
	private AtomicBoolean warmedUp = new AtomicBoolean(false);
	private AtomicInteger noOfRequests = new AtomicInteger(0);
	private MetricRegistry metricRegistry;
	private Configuration configuration;
	private ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

	private volatile MeasurementSession measurementSession;

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
		this.measurementSession = measurementSession;
		StageMonitor.startMonitoring(measurementSession);
	}

	public <T extends RequestTrace> RequestInformation<T> monitor(MonitoredRequest<T> monitoredRequest) throws Exception {
		if (measurementSession == null) {
			createMeasurementSession();
		}

		if (measurementSession.getInstanceName() == null && noOfRequests.get() == 0) {
			getInstanceNameFromExecution(monitoredRequest);
		}

		RequestInformation<T> ei = new RequestInformation<T>();
		final boolean monitor = configuration.isCollectRequestStats() && isWarmedUp();
		if (monitor) {
			beforeExecution(monitoredRequest, ei);
		}
		try {
			ei.executionResult = monitoredRequest.execute();
			return ei;
		} catch (Exception e) {
			ei.requestTrace.setException(e);
			throw e;
		} finally {
			if (monitor) {
				afterExecution(monitoredRequest, ei);
			}
		}
	}

	/*
	 * In case the instance name is not set by configuration, try to read from monitored execution
	 * (e.g. the domain name from a HTTP request)
	 */
	private synchronized void getInstanceNameFromExecution(MonitoredRequest<?> monitoredRequest) {
		if (measurementSession.getInstanceName() == null) {
			measurementSession.setInstanceName(monitoredRequest.getInstanceName());
			StageMonitor.startMonitoring(measurementSession);
		}
	}

	private synchronized void createMeasurementSession() {
		if (measurementSession == null) {
			MeasurementSession session = new MeasurementSession();
			session.setHostName(getHostName());
			session.setInstanceName(configuration.getInstanceName());
			session.setApplicationName(configuration.getApplicationName());
			setMeasurementSession(session);
		}
	}

	private <T extends RequestTrace> void beforeExecution(MonitoredRequest<T> monitoredRequest, RequestInformation<T> ei) {
		ei.requestTrace = monitoredRequest.createRequestTrace();
		try {
			ei.requestTrace.setMeasurementSession(measurementSession);
			if (ei.monitorThisExecution()) {
				if (actualRequestName.get() != null) {
					ei.forwardedExecution = true;
					if (!monitoredRequest.isMonitorForwardedExecutions()) {
						ei.requestTrace = null;
						return;
					}
				}
				actualRequestName.set(ei.requestTrace.getName());
				ei.timer = metricRegistry.timer(name("request", "total", ei.getTimerName()));
				if (ei.profileThisExecution()) {
					final CallStackElement root = Profiler.activateProfiling();
					ei.requestTrace.setCallStack(root);
				}
			}
		} catch (RuntimeException e) {
			logger.warn(e.getMessage() + " (this exception is ignored) actualRequestName=" + actualRequestName.get() +
					ei.toString(), e);
		}
	}

	private <T extends RequestTrace> void afterExecution(MonitoredRequest<T> monitoredRequest,
															 RequestInformation<T> ei) {
		try {
			if (ei.monitorThisExecution()) {
				// if forwarded executions are not monitored, ei.requestTrace would be null
				if (!ei.isForwardingExecution()) {
					final long executionTime = System.nanoTime() - ei.start;
					final long cpuTime = getCpuTime() - ei.startCpu;
					ei.requestTrace.setExecutionTime(NANOSECONDS.toMillis(executionTime));
					ei.requestTrace.setExecutionTimeCpu(NANOSECONDS.toMillis(cpuTime));
					monitoredRequest.onPostExecute(ei.requestTrace);

					if (ei.requestTrace.getCallStack() != null) {
						Profiler.stop("total");
						reportCallStack(ei.requestTrace, configuration.getElasticsearchUrl());
					}
					if (ei.timer != null) {
						ei.timer.update(executionTime, NANOSECONDS);
						if (configuration.isCollectCpuTime()) {
							metricRegistry.timer(name("request", "cpu", ei.getTimerName())).update(cpuTime, NANOSECONDS);
						}
						if (ei.requestTrace.isError()) {
							metricRegistry.meter(name("request", "error", ei.getTimerName())).mark();
						}
					}
				} else {
					if (ei.timer.getCount() == 0) {
						metricRegistry.remove(name("request", "total", ei.getTimerName()));
					}
				}
			}
		} catch (RuntimeException e) {
			logger.warn(e.getMessage() + " (this exception is ignored) actualRequestName=" + actualRequestName.get()
					+ ei.toString(), e);
		} finally {
			/*
			 * The forwarded execution is executed in the same thread.
			 * Only remove the thread local on the topmost execution context,
			 * otherwise ei.isForwardingExecution() doesn't work
			 */
			if (!ei.forwardedExecution) {
				actualRequestName.remove();
			}
			if (ei.requestTrace != null) {
				Profiler.clearMethodCallParent();
			}
		}
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

		private String getTimerName() {
			return name(GraphiteSanitizer.sanitizeGraphiteMetricSegment(requestTrace.getName()));
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

}
