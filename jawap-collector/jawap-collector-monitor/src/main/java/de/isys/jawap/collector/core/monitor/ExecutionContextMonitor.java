package de.isys.jawap.collector.core.monitor;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import de.isys.jawap.collector.core.Configuration;
import de.isys.jawap.collector.core.JawapApplicationContext;
import de.isys.jawap.collector.core.monitor.rest.ExecutionContextRestClient;
import de.isys.jawap.collector.core.rest.MeasurementSessionRestClient;
import de.isys.jawap.collector.profiler.ExecutionContextLogger;
import de.isys.jawap.collector.profiler.Profiler;
import de.isys.jawap.entities.MeasurementSession;
import de.isys.jawap.entities.profiler.CallStackElement;
import de.isys.jawap.entities.profiler.ExecutionContext;
import de.isys.jawap.util.GraphiteEncoder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;

public class ExecutionContextMonitor {

	private static final Log logger = LogFactory.getLog(ExecutionContextMonitor.class);
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

	private volatile MeasurementSession measurementSession;

	private MeasurementSessionRestClient measurementSessionRestClient;
	private ExecutionContextRestClient executionContextRestClient = new ExecutionContextRestClient();
	private String measurementSessionLocation;
	private Date endOfWarmup;

	public ExecutionContextMonitor() {
		this(JawapApplicationContext.getConfiguration());
	}

	public ExecutionContextMonitor(Configuration configuration) {
		measurementSessionRestClient = new MeasurementSessionRestClient(configuration.getServerUrl());
		warmupRequests = configuration.getNoOfWarmupRequests();
		this.metricRegistry = JawapApplicationContext.getMetricRegistry();
		this.configuration = configuration;
		endOfWarmup = new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(configuration.getWarmupSeconds()));
	}

	public void setMeasurementSession(MeasurementSession measurementSession) {
		this.measurementSession = measurementSession;
		if (JawapApplicationContext.startMonitoring(measurementSession)) {
			measurementSessionLocation = measurementSessionRestClient.saveMeasurementSession(measurementSession);
		}
	}

	public <T extends ExecutionContext> ExecutionInformation<T> monitor(MonitoredExecution<T> monitoredExecution) throws Exception {
		if (measurementSession == null) {
			createMeasurementSession();
		}

		if (measurementSession.getInstanceName() == null && noOfRequests.get() == 0) {
			getInstanceNameFromExecution(monitoredExecution);
		}

		if (configuration.isCollectRequestStats() && isWarmedUp()) {
			ExecutionInformation<T> ei = new ExecutionInformation<T>();
			beforeExecution(monitoredExecution, ei);
			try {
				ei.executionResult = monitoredExecution.execute();
			} catch (Exception e) {
				ei.exceptionThrown = true;
				throw e;
			} finally {
				afterExecution(monitoredExecution, ei);
				return ei;
			}
		} else {
			monitoredExecution.execute();
			return null;
		}
	}

	/*
	 * In case the instance name is not set by configuration, try to read from monitored execution
	 * (e.g. the domain name from a HTTP request)
	 */
	private synchronized void getInstanceNameFromExecution(MonitoredExecution<?> monitoredExecution) {
		if (measurementSession.getInstanceName() == null) {
			measurementSession.setInstanceName(monitoredExecution.getInstanceName());
			JawapApplicationContext.startMonitoring(measurementSession);
			measurementSessionLocation = measurementSessionRestClient.saveMeasurementSession(measurementSession);
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
			String requestName = ei.executionContext.getName();
			if (requestName == null || requestName.isEmpty()) {
				throw new RuntimeException("requestName is null or empty! '" + requestName + "'");
			}
			if (actualRequestName.get() != null) {
				ei.forwardedExecution = true;
				if (!monitoredExecution.isMonitorForwardedExecutions()) {
					ei.executionContext = null;
					return;
				}
			}
			actualRequestName.set(requestName);
			ei.executionContext.setName(requestName);
			ei.timer = metricRegistry.timer(ei.getTimerName());
			if (ei.profileThisRequest()) {
				final CallStackElement root = Profiler.activateProfiling();
				ei.executionContext.setCallStack(root);
			}
		} catch (RuntimeException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private <T extends ExecutionContext> void afterExecution(MonitoredExecution<T> monitoredExecution,
															 ExecutionInformation<T> ei) {
		try {
			if (ei.executionContext != null) {
				// if forwarded executions are not monitored, ei.executionContext would be null
				if (!ei.isForwardingExecution()) {
					long executionTime = System.nanoTime() - ei.start;
					ei.executionContext.setError(ei.exceptionThrown);
					ei.executionContext.setExecutionTime(executionTime);
					monitoredExecution.onPostExecute(ei.executionContext);

					if (ei.executionContext.getCallStack() != null) {
						Profiler.stop("total");
						reportCallStack(ei.executionContext);
					}
					if (ei.timer != null) {
						ei.timer.update(executionTime, TimeUnit.NANOSECONDS);
						if (ei.executionContext.isError()) {
							metricRegistry.meter(name(ei.getTimerName(), "error")).mark();
						}
					}
				} else {
					if (ei.timer.getCount() == 0) {
						metricRegistry.remove(ei.getTimerName());
					}
				}
			}
		} catch (RuntimeException e) {
			logger.error(e.getMessage(), e);
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

	private <T extends ExecutionContext> void reportCallStack(T executionContext) {
		if (configuration.isReportCallStacksToServer()) {
			executionContextRestClient.saveRequestContext(measurementSessionLocation, executionContext);
		}
		if (configuration.isLogCallStacks()) {
			executionContextLogger.logStats(executionContext);
		}
	}

	public void onPreDestroy() {
		measurementSession.setEndOfSession(new Date());
		measurementSessionRestClient.updateMeasurementSession(measurementSession, measurementSessionLocation);
	}

	private boolean isWarmedUp() {
		if (!warmedUp.get()) {
			warmedUp.set(warmupRequests < noOfRequests.incrementAndGet() && new Date().after(endOfWarmup));
			return warmedUp.get();
		} else {
			return true;
		}
	}

	public static String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	public class ExecutionInformation<T extends ExecutionContext> {
		Timer timer = null;
		T executionContext = null;
		boolean exceptionThrown = false;
		long start = System.nanoTime();
		boolean forwardedExecution = false;
		Object executionResult = null;

		private boolean profileThisRequest() {
			int callStackEveryXRequestsToGroup = configuration.getCallStackEveryXRequestsToGroup();
			if (callStackEveryXRequestsToGroup == 1) return true;
			if (callStackEveryXRequestsToGroup < 1) return false;
			if (timer.getCount() == 0) return false;
			return timer.getCount() % callStackEveryXRequestsToGroup == 0;
		}

		private String getTimerName() {
			return name("request", GraphiteEncoder.encodeForGraphite(executionContext.getName()));
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
			return !actualRequestName.get().equals(executionContext.getName());
		}

		public Object getExecutionResult() {
			return executionResult;
		}
	}

}
