package de.isys.jawap.collector.periodic;

import de.isys.jawap.collector.AbstractExclusionFilter;
import de.isys.jawap.collector.Configuration;
import de.isys.jawap.collector.facade.PerformanceMeasuringFacade;
import de.isys.jawap.collector.model.HttpRequestStats;
import de.isys.jawap.collector.model.MethodCallStats;
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
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

// TODO as Aspect of Servlet+.service(..) ?
public class PerformanceMonitorFilter extends AbstractExclusionFilter {

	private final Log logger = LogFactory.getLog(getClass());

	private PerformanceMeasuringFacade performanceMeasuringFacade;

	private PerformanceMeasurementSession performanceMeasurementSession;

	private CpuUtilisationWatch cpuWatch = new CpuUtilisationWatch();

	private int warmupRequests = 0;
	private boolean warmedUp = false;
	private AtomicInteger noOfRequests = new AtomicInteger(0);

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
			HttpRequestStats requestStats = getRequestStats(httpServletRequest);

			MethodCallStats root = null;
			if (Configuration.METHOD_PERFORMANCE_STATS) {
				root = new MethodCallStats(null);
				Profiler.setCurrentRequestStats(requestStats);
				Profiler.setMethodCallRoot(root);
			}

			filterChain.doFilter(servletRequest, servletResponse);

			long stop = System.currentTimeMillis();
			requestStats.setExecutionTime(stop - start);
			requestStats.setStatusCode(httpServletResponse.getStatus());

			if (root != null) {
				requestStats.setMethodCallStats(root.getChildren());
				Profiler.clearStats();
				Profiler.clearCurrentRequestStats();
			}
			performanceMeasuringFacade.save(requestStats);

		} else {
			filterChain.doFilter(servletRequest, servletResponse);
		}
	}

	private boolean isWarmedUp() {
		if (!warmedUp) {
			warmedUp = warmupRequests < noOfRequests.incrementAndGet();
		}
		return warmedUp;
	}

	private HttpRequestStats getRequestStats(HttpServletRequest httpServletRequest) {
		HttpRequestStats requestStats = new HttpRequestStats();
		requestStats.setPerformanceMeasurementSession(performanceMeasurementSession);
		requestStats.setUrl(httpServletRequest.getRequestURI());
		requestStats.setQueryParams(httpServletRequest.getQueryString());
		return requestStats;
	}

	@PreDestroy
	public void onPreDestroy() {
		if (!Configuration.PERFORMANCE_STATS_LOG_ONLY && performanceMeasurementSession.getId() != null) {
			performanceMeasurementSession.setEndOfSession(new Date());
			performanceMeasurementSession.setCpuUsagePercent(Float.valueOf(cpuWatch.getCpuUsagePercent()));
			GCStatsUtil.GCStats gcStats = GCStatsUtil.getGCStats();
			performanceMeasurementSession.setGarbageCollectionsCount(Long.valueOf(gcStats.collectionCount));
			performanceMeasurementSession.setGarbageCollectionTime(Long.valueOf(gcStats.garbageCollectionTime));
			performanceMeasuringFacade.update(performanceMeasurementSession);
		}

		Profiler.clearAllThreadLoals();
	}

	public PerformanceMeasurementSession getPerformanceMeasurementSession() {
		return performanceMeasurementSession;
	}
}
