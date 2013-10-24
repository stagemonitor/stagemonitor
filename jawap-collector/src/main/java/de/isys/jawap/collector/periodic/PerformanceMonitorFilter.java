package de.isys.jawap.collector.periodic;

import de.isys.jawap.collector.AbstractExclusionFilter;
import de.isys.jawap.collector.Configuration;
import de.isys.jawap.collector.facade.PerformanceMeasuringFacade;
import de.isys.jawap.collector.model.HttpRequestContext;
import de.isys.jawap.collector.model.PerformanceMeasurementSession;
import de.isys.jawap.collector.profile.Profiler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

// TODO as Aspect of Servlet+.service(..) or HttpServletRequest.new ?
public class PerformanceMonitorFilter extends AbstractExclusionFilter {

	private final Log logger = LogFactory.getLog(getClass());

	private Method statusCodeMethod;

	private PerformanceMeasuringFacade performanceMeasuringFacade;

	private PerformanceMeasurementSession performanceMeasurementSession;

	private CpuUtilisationWatch cpuWatch = new CpuUtilisationWatch();

	private int warmupRequests = 0;
	private boolean warmedUp = false;
	private AtomicInteger noOfRequests = new AtomicInteger(0);

	public PerformanceMonitorFilter() {
		try {
			statusCodeMethod = HttpServletResponse.class.getMethod("getStatus");
		} catch (NoSuchMethodException e) {
			// we are in a pre servlet 3.0 environment
			statusCodeMethod = null;
		}
	}

	@PostConstruct
	public void onPostConstruct() {
		cpuWatch.start();
		performanceMeasurementSession = new PerformanceMeasurementSession();
		if (!Configuration.PERFORMANCE_STATS_LOG_ONLY) {
			try {
				performanceMeasuringFacade.save(performanceMeasurementSession);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		warmupRequests = Configuration.PERFORMANCE_STATS_WARMUP_REQUESTS;
	}

	@Override
	public void doFilterInternal(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws
			IOException, ServletException {

		if (Configuration.REQUEST_PERFORMANCE_STATS && isWarmedUp()
				&& servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse) {

			HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
			HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
			long start = System.currentTimeMillis();
			HttpRequestContext requestStats = getRequestStats(httpServletRequest);

			if (Configuration.METHOD_PERFORMANCE_STATS) {
				Profiler.setExecutionContext(requestStats);
			}
			try {
				filterChain.doFilter(servletRequest, servletResponse);
			} finally {
				long stop = System.currentTimeMillis();
				requestStats.setExecutionTime(stop - start);
				requestStats.setStatusCode(getStatusCode(httpServletResponse));
				performanceMeasuringFacade.save(requestStats);
			}
		} else {
			filterChain.doFilter(servletRequest, servletResponse);
		}
	}

	private Integer getStatusCode(HttpServletResponse httpServletResponse) {
		if (statusCodeMethod != null) {
			try {
				return (Integer) statusCodeMethod.invoke(httpServletResponse);
			} catch (IllegalAccessException e) {
				logger.error(e.getMessage(), e);
			} catch (InvocationTargetException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return null;
	}

	private boolean isWarmedUp() {
		if (!warmedUp) {
			warmedUp = warmupRequests < noOfRequests.incrementAndGet();
		}
		return warmedUp;
	}

	private HttpRequestContext getRequestStats(HttpServletRequest httpServletRequest) {
		HttpRequestContext requestStats = new HttpRequestContext();
		requestStats.setPerformanceMeasurementSession(performanceMeasurementSession);
		requestStats.setUrl(httpServletRequest.getRequestURI());
		requestStats.setQueryParams(httpServletRequest.getQueryString());
		return requestStats;
	}

	@PreDestroy
	public void onPreDestroy() {
		if (!Configuration.PERFORMANCE_STATS_LOG_ONLY && performanceMeasurementSession.getId() != null) {
			performanceMeasurementSession.setEndOfSession(new Date());
			performanceMeasurementSession.setCpuUsagePercent(cpuWatch.getCpuUsagePercent());
			GCStatsUtil.GCStats gcStats = GCStatsUtil.getGCStats();
			performanceMeasurementSession.setGarbageCollectionsCount(gcStats.collectionCount);
			performanceMeasurementSession.setGarbageCollectionTime(gcStats.garbageCollectionTime);
			performanceMeasuringFacade.update(performanceMeasurementSession);
		}
	}

	public PerformanceMeasurementSession getPerformanceMeasurementSession() {
		return performanceMeasurementSession;
	}
}
