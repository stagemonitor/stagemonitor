package de.isys.jawap.collector.core.monitor;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import de.isys.jawap.collector.core.ApplicationContext;
import de.isys.jawap.collector.core.Configuration;
import de.isys.jawap.collector.core.rest.MeasurementSessionRestClient;
import de.isys.jawap.collector.profiler.ExecutionContextLogger;
import de.isys.jawap.collector.profiler.Profiler;
import de.isys.jawap.collector.web.rest.HttpRequestContextRestClient;
import de.isys.jawap.entities.MeasurementSession;
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

	private int warmupRequests = 0;
	private AtomicBoolean warmedUp = new AtomicBoolean(false);
	private AtomicInteger noOfRequests = new AtomicInteger(0);
	private MetricRegistry metricRegistry;
	private Configuration configuration;

	private MeasurementSession measurementSession;

	private MeasurementSessionRestClient measurementSessionRestClient;
	private String measurementSessionLocation;
	private HttpRequestContextRestClient httpRequestContextRestClient;
	private Date endOfWarmup;

	public ExecutionContextMonitor(Configuration configuration) {
		measurementSessionRestClient = new MeasurementSessionRestClient(configuration.getServerUrl());
		httpRequestContextRestClient = new HttpRequestContextRestClient();
		warmupRequests = configuration.getNoOfWarmupRequests();
		this.metricRegistry = ApplicationContext.getMetricRegistry();
		this.configuration = configuration;
		endOfWarmup = new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(configuration.getWarmupSeconds()));
	}

	public void setMeasurementSession(MeasurementSession measurementSession) {
		this.measurementSession = measurementSession;
		ApplicationContext.startMonitoring(measurementSession);
		if (measurementSession.isInitialized()) {
			measurementSessionLocation = measurementSessionRestClient.saveMeasurementSession(measurementSession);
		}
	}

	public <T extends ExecutionContext> void monitor(MonitoredExecution<T> monitoredExecution) throws Exception {
		// in case the instance name is not set by configuration
		if (measurementSession.getInstanceName() == null && noOfRequests.get() == 0) {
			measurementSession.setInstanceName(monitoredExecution.getInstanceName());
			ApplicationContext.startMonitoring(measurementSession);
			measurementSessionLocation = measurementSessionRestClient.saveMeasurementSession(measurementSession);
		}

		if (configuration.isCollectRequestStats() && isWarmedUp()) {

			Timer timer = null;
			T requestContext = null;
			boolean exceptionThrown = false;
			long start = System.nanoTime();

			if (configuration.isRequestTimerEnabled()) {
				requestContext = monitoredExecution.getExecutionContext();
				String requestName = monitoredExecution.getRequestName();
				requestContext.setName(requestName);
				timer = metricRegistry.timer(getTimerName(requestName));
				if (profileThisRequest(timer)) {
					Profiler.setExecutionContext(requestContext);
					Profiler.start();
				}
			}
			try {
				monitoredExecution.execute();
			} catch (Exception e) {
				exceptionThrown = true;
				throw e;
			} finally {
				if (requestContext != null) {
					long executionTime = System.nanoTime() - start;
					requestContext.setFailed(exceptionThrown);
					requestContext.setExecutionTime(executionTime);
					monitoredExecution.onPostExecute(requestContext);

					if (requestContext.getCallStack() != null) {
						Profiler.stop(null, "total");
						reportCallStack(requestContext);
					}
					if (timer != null) {
						timer.update(executionTime, TimeUnit.NANOSECONDS);
						if (requestContext.isFailed()) {
							metricRegistry.counter(name(getTimerName(requestContext.getName()), "failed")).inc();
						}
					}
				}
			}
		} else {
			monitoredExecution.execute();
		}
	}

	private <T extends ExecutionContext> String getTimerName(String requestName) {
		return name("request", GraphiteEncoder.encodeForGraphite(requestName));
	}

	private <T extends ExecutionContext> void reportCallStack(T requestContext) {
		if (configuration.isReportCallStacksToServer()) {
			httpRequestContextRestClient.saveRequestContext(measurementSessionLocation, requestContext);
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
