package org.stagemonitor.collector.core.monitor;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.collector.core.Configuration;
import org.stagemonitor.collector.core.MeasurementSession;
import org.stagemonitor.collector.core.StageMonitor;
import org.stagemonitor.collector.core.rest.RestClient;
import org.stagemonitor.collector.core.util.GraphiteEncoder;
import org.stagemonitor.collector.profiler.CallStackElement;
import org.stagemonitor.collector.profiler.ExecutionContextLogger;
import org.stagemonitor.collector.profiler.Profiler;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class ExecutionContextMonitor {

	private static final Logger logger = LoggerFactory.getLogger(ExecutionContextMonitor.class);
	private final ExecutionContextLogger executionContextLogger = new ExecutionContextLogger();

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

	public ExecutionContextMonitor() {
		this(StageMonitor.getConfiguration());
	}

	public ExecutionContextMonitor(Configuration configuration) {
		warmupRequests = configuration.getNoOfWarmupRequests();
		this.metricRegistry = StageMonitor.getMetricRegistry();
		this.configuration = configuration;
		endOfWarmup = new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(configuration.getWarmupSeconds()));
	}

	public void setMeasurementSession(MeasurementSession measurementSession) {
		this.measurementSession = measurementSession;
		StageMonitor.startMonitoring(measurementSession);
	}

	public <T extends ExecutionContext> ExecutionInformation<T> monitor(MonitoredExecution<T> monitoredExecution) throws Exception {
		if (measurementSession == null) {
			createMeasurementSession();
		}

		if (measurementSession.getInstanceName() == null && noOfRequests.get() == 0) {
			getInstanceNameFromExecution(monitoredExecution);
		}

		ExecutionInformation<T> ei = new ExecutionInformation<T>();
		final boolean monitor = configuration.isCollectRequestStats() && isWarmedUp();
		if (monitor) {
			beforeExecution(monitoredExecution, ei);
		}
		try {
			ei.executionResult = monitoredExecution.execute();
			return ei;
		} catch (Exception e) {
			ei.executionContext.setException(e);
			throw e;
		} finally {
			if (monitor) {
				afterExecution(monitoredExecution, ei);
			}
		}
	}

	/*
	 * In case the instance name is not set by configuration, try to read from monitored execution
	 * (e.g. the domain name from a HTTP request)
	 */
	private synchronized void getInstanceNameFromExecution(MonitoredExecution<?> monitoredExecution) {
		if (measurementSession.getInstanceName() == null) {
			measurementSession.setInstanceName(monitoredExecution.getInstanceName());
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

	private <T extends ExecutionContext> void beforeExecution(MonitoredExecution<T> monitoredExecution, ExecutionInformation<T> ei) {
		try {
			ei.executionContext = monitoredExecution.createExecutionContext();
			ei.executionContext.setMeasurementSession(measurementSession);
			if (ei.monitorThisExecution()) {
				if (actualRequestName.get() != null) {
					ei.forwardedExecution = true;
					if (!monitoredExecution.isMonitorForwardedExecutions()) {
						ei.executionContext = null;
						return;
					}
				}
				actualRequestName.set(ei.executionContext.getName());
				ei.timer = metricRegistry.timer(name("request", "total", ei.getTimerName()));
				if (ei.profileThisExecution()) {
					final CallStackElement root = Profiler.activateProfiling();
					ei.executionContext.setCallStack(root);
				}
			}
		} catch (RuntimeException e) {
			logger.warn(e.getMessage() + " (this exception is ignored) actualRequestName=" + actualRequestName.get() +
					ei.toString(), e);
		}
	}

	private <T extends ExecutionContext> void afterExecution(MonitoredExecution<T> monitoredExecution,
															 ExecutionInformation<T> ei) {
		try {
			if (ei.monitorThisExecution()) {
				// if forwarded executions are not monitored, ei.executionContext would be null
				if (!ei.isForwardingExecution()) {
					final long executionTime = System.nanoTime() - ei.start;
					final long cpuTime = getCpuTime() - ei.startCpu;
					ei.executionContext.setExecutionTime(NANOSECONDS.toMillis(executionTime));
					ei.executionContext.setCpuTime(NANOSECONDS.toMillis(cpuTime));
					monitoredExecution.onPostExecute(ei.executionContext);

					if (ei.executionContext.getCallStack() != null) {
						Profiler.stop("total");
						reportCallStack(ei.executionContext, configuration.getElasticsearchUrl());
					}
					if (ei.timer != null) {
						ei.timer.update(executionTime, NANOSECONDS);
						if (configuration.isCollectCpuTime()) {
							metricRegistry.timer(name("request", "cpu", ei.getTimerName())).update(cpuTime, NANOSECONDS);
						}
						if (ei.executionContext.isError()) {
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
			 * the forwarded execution is executed in the same thread
			 * only remove the thread local on the topmost execution context
			 * otherwise ei.isForwardingExecution() doesn't work
			 *
			 * TODO really?
			 */
			if (!ei.forwardedExecution) {
				actualRequestName.remove();
			}
			if (ei.executionContext != null) {
				Profiler.clearMethodCallParent();
			}
		}
	}

	private <T extends ExecutionContext> void reportCallStack(T executionContext, String serverUrl) {
		if (serverUrl != null && !serverUrl.isEmpty()) {
			String path = String.format("/stagemonitor-%s/executions/%s",
					new SimpleDateFormat("yyyy.MM.dd").format(new Date()), executionContext.getId());
			final String ttl = configuration.getCallStacksTimeToLive();
			if (ttl != null && !ttl.isEmpty()) {
				path += "?ttl=" + ttl;
			}
			RestClient.sendAsJsonAsync(serverUrl, path, "PUT", executionContext);
		}
		if (configuration.isLogCallStacks()) {
			executionContextLogger.logStats(executionContext);
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

	public class ExecutionInformation<T extends ExecutionContext> {
		Timer timer = null;
		T executionContext = null;
		long start = System.nanoTime();
		long startCpu = getCpuTime();
		boolean forwardedExecution = false;
		Object executionResult = null;

		private boolean profileThisExecution() {
			int callStackEveryXRequestsToGroup = configuration.getCallStackEveryXRequestsToGroup();
			if (callStackEveryXRequestsToGroup == 1) return true;
			if (callStackEveryXRequestsToGroup < 1) return false;
			if (timer.getCount() == 0) return false;
			return timer.getCount() % callStackEveryXRequestsToGroup == 0;
		}

		private boolean monitorThisExecution() {
			return executionContext != null && executionContext.getName() != null && !executionContext.getName().isEmpty();
		}

		private String getTimerName() {
			return name(GraphiteEncoder.encodeForGraphite(executionContext.getName()));
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
		 * (monitored means monitored by {@link ExecutionContextMonitor}).
		 * Method a() is the forwarding execution, Method b() is the forwarded execution.
		 *
		 * @return true, if this request is a forwarding request, false otherwise
		 */
		private boolean isForwardingExecution() {
			return !executionContext.getName().equals(actualRequestName.get());
		}

		public Object getExecutionResult() {
			return executionResult;
		}

		@Override
		public String toString() {
			return "ExecutionInformation{" +
					"timer=" + timer +
					", executionContext=" + executionContext +
					", start=" + start +
					", startCpu=" + startCpu +
					", forwardedExecution=" + forwardedExecution +
					", executionResult=" + executionResult +
					'}';
		}
	}

}
