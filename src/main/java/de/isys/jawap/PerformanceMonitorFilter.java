package de.isys.jawap;

import de.isys.jawap.collectors.CpuUtilisationWatch;
import de.isys.jawap.collectors.GCStatsUtil;
import de.isys.jawap.instrument.PerformanceMonitorAspect;
import de.isys.jawap.model.HttpRequestStats;
import de.isys.jawap.model.MethodCallStats;
import de.isys.jawap.model.PerformanceMeasurementSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.web.util.RequestMatcher;
import org.webstage.shop.core.configuration.DBProperties;
import org.webstage.shop.facade.PerformanceMeasuringFacade;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceMonitorFilter extends AbstractExclusionFilter {

	private final Log logger = LogFactory.getLog(getClass());

	@Resource(name = "performanceMeasuringFacade")
	private PerformanceMeasuringFacade performanceMeasuringFacade;

	private PerformanceMeasurementSession performanceMeasurementSession;

	private CpuUtilisationWatch cpuWatch = new CpuUtilisationWatch();

	private List<RequestMatcher> excludedPaths = new ArrayList<RequestMatcher>();
	private int warmupRequests = 0;
	private boolean warmedUp = false;
	private AtomicInteger noOfRequests = new AtomicInteger(0);

	@PostConstruct
	public void onPostConstruct() {
		cpuWatch.start();
		performanceMeasurementSession = new PerformanceMeasurementSession();
		if (!DBProperties.PERFORMANCE_STATS_LOG_ONLY.getBoolean()) {
			try {
				performanceMeasuringFacade.save(performanceMeasurementSession);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		warmupRequests = DBProperties.PERFORMANCE_STATS_WARMUP_REQUESTS.getIntValue();
	}

	@Override
	public void doFilterInternal(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws
			IOException, ServletException {

		if (DBProperties.REQUEST_PERFORMANCE_STATS.getBoolean() && isWarmedUp()
				&& servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse) {

			HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
			HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
			long start = System.currentTimeMillis();
			HttpRequestStats requestStats = getRequestStats(httpServletRequest);

			MethodCallStats root = null;
			if (DBProperties.METHOD_PERFORMANCE_STATS.getBoolean()) {
				root = new MethodCallStats();
				PerformanceMonitorAspect.setCurrentRequestStats(requestStats);
				PerformanceMonitorAspect.setMethodCallRoot(root);
			}

			filterChain.doFilter(servletRequest, servletResponse);

			long stop = System.currentTimeMillis();
			requestStats.setExecutionTime(stop - start);
			requestStats.setStatusCode(Integer.valueOf(httpServletResponse.getStatus()));

			if (root != null) {
				requestStats.setMethodCallStats(root.getChildren());
				PerformanceMonitorAspect.clearStats();
				PerformanceMonitorAspect.clearCurrentRequestStats();
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
		if (!DBProperties.PERFORMANCE_STATS_LOG_ONLY.getBoolean() && performanceMeasurementSession.getId() != null) {
			performanceMeasurementSession.setEndOfSession(new Date());
			performanceMeasurementSession.setCpuUsagePercent(Float.valueOf(cpuWatch.getCpuUsagePercent()));
			GCStatsUtil.GCStats gcStats = GCStatsUtil.getGCStats();
			performanceMeasurementSession.setGarbageCollectionsCount(Long.valueOf(gcStats.collectionCount));
			performanceMeasurementSession.setGarbageCollectionTime(Long.valueOf(gcStats.garbageCollectionTime));
			performanceMeasuringFacade.update(performanceMeasurementSession);
		}

		PerformanceMonitorAspect.clearAllThreadLoals();
	}

	public List<RequestMatcher> getExcludedPaths() {
		return excludedPaths;
	}

	public PerformanceMeasurementSession getPerformanceMeasurementSession() {
		return performanceMeasurementSession;
	}
}
