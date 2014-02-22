package de.isys.jawap.collector.core.monitor;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import de.isys.jawap.collector.core.JawapApplicationContext;
import de.isys.jawap.collector.core.Configuration;
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
	 * helps to detect, if this request is the 'real' one or just the forwarding one.
	 */
	private static ThreadLocal<String> actualRequestName = new ThreadLocal<String>();

	private int warmupRequests = 0;
	private AtomicBoolean warmedUp = new AtomicBoolean(false);
	private AtomicInteger noOfRequests = new AtomicInteger(0);
	private MetricRegistry metricRegistry;
	private Configuration configuration;

	private MeasurementSession measurementSession;

	private MeasurementSessionRestClient measurementSessionRestClient;
	private String measurementSessionLocation;
	private Date endOfWarmup;

	public ExecutionContextMonitor(Configuration configuration) {
		measurementSessionRestClient = new MeasurementSessionRestClient(configuration.getServerUrl());
		warmupRequests = configuration.getNoOfWarmupRequests();
		this.metricRegistry = JawapApplicationContext.getMetricRegistry();
		this.configuration = configuration;
		endOfWarmup = new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(configuration.getWarmupSeconds()));
	}

	public void setMeasurementSession(MeasurementSession measurementSession) {
		this.measurementSession = measurementSession;
		JawapApplicationContext.startMonitoring(measurementSession);
		if (measurementSession.isInitialized()) {
			measurementSessionLocation = measurementSessionRestClient.saveMeasurementSession(measurementSession);
		}
	}

	public <T extends ExecutionContext> void monitor(MonitoredExecution<T> monitoredExecution) throws Exception {
		// in case the instance name is not set by configuration
		if (measurementSession.getInstanceName() == null && noOfRequests.get() == 0) {
			measurementSession.setInstanceName(monitoredExecution.getInstanceName());
			JawapApplicationContext.startMonitoring(measurementSession);
			measurementSessionLocation = measurementSessionRestClient.saveMeasurementSession(measurementSession);
		}

		if (configuration.isCollectRequestStats() && isWarmedUp()) {

			Timer timer = null;
			T executionContext = null;
			boolean exceptionThrown = false;
			long start = System.nanoTime();
			boolean forwardedExecution = false;
			try {
				executionContext = monitoredExecution.getExecutionContext();
				String requestName = monitoredExecution.getRequestName();
				if (actualRequestName.get() != null) {
					forwardedExecution = true;
				}
				actualRequestName.set(requestName);
				executionContext.setName(requestName);
				timer = metricRegistry.timer(getTimerName(requestName));
				if (profileThisRequest(timer)) {
					final CallStackElement root = Profiler.activateProfiling();
					executionContext.setCallStack(root);
				}
			} catch (RuntimeException e) {
				logger.error(e.getMessage(), e);
			}
			try {
				monitoredExecution.execute();
			} catch (Exception e) {
				exceptionThrown = true;
				throw e;
			} finally {
				try {
					if (executionContext != null && !isForwardingExecution(executionContext.getName())) {
						long executionTime = System.nanoTime() - start;
						executionContext.setError(exceptionThrown);
						executionContext.setExecutionTime(executionTime);
						monitoredExecution.onPostExecute(executionContext);

						if (executionContext.getCallStack() != null) {
							Profiler.stop("total");
							reportCallStack(monitoredExecution, executionContext);
						}
						if (timer != null) {
							timer.update(executionTime, TimeUnit.NANOSECONDS);
							if (executionContext.isError()) {
								metricRegistry.meter(name(getTimerName(executionContext.getName()), "error")).mark();
							}
						}
					} else if (isForwardingExecution(executionContext.getName())) {
						// don't remove forwarding request from timer, if it is forwarding only sometimes
						if (timer.getCount() == 0) {
							metricRegistry.remove(getTimerName(executionContext.getName()));
						}
					}
				} catch (RuntimeException e) {
					logger.error(e.getMessage(), e);
				}
				if (!forwardedExecution) {
					actualRequestName.remove();
				}
				Profiler.clearMethodCallParent();
			}
		} else {
			monitoredExecution.execute();
		}
	}

	private boolean isForwardingExecution(String thisExecutionsName) {
		return !actualRequestName.get().equals(thisExecutionsName);
	}

	private <T extends ExecutionContext> String getTimerName(String requestName) {
		return name("request", GraphiteEncoder.encodeForGraphite(requestName));
	}

	private <T extends ExecutionContext> void reportCallStack(MonitoredExecution<T> monitoredExecution, T requestContext) {
		if (configuration.isReportCallStacksToServer()) {
			monitoredExecution.reportCallStackToServer(measurementSessionLocation, requestContext);
		}
		if (configuration.isLogCallStacks()) {
			executionContextLogger.logStats(requestContext);
		}
	}

	public void onPreDestroy() {
		measurementSession.setEndOfSession(new Date());
		measurementSessionRestClient.updateMeasurementSession(measurementSession, measurementSessionLocation);
	}

	private boolean profileThisRequest(Timer timer) {
		int callStackEveryXRequestsToGroup = configuration.getCallStackEveryXRequestsToGroup();
		if (callStackEveryXRequestsToGroup == 1) return true;
		if (callStackEveryXRequestsToGroup < 1) return false;
		if (timer.getCount() == 0) return false;
		return timer.getCount() % callStackEveryXRequestsToGroup == 0;
	}

	private boolean isWarmedUp() {
		if (!warmedUp.get()) {
			warmedUp.set(warmupRequests < noOfRequests.incrementAndGet() && new Date().after(endOfWarmup));
		}
		return warmedUp.get();
	}

	public static String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

}
