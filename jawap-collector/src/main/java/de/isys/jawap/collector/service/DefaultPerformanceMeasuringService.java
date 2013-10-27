package de.isys.jawap.collector.service;

import de.isys.jawap.collector.dao.PerformanceMeasuringDao;
import de.isys.jawap.collector.model.HttpRequestContext;
import de.isys.jawap.collector.model.MethodCallStats;
import de.isys.jawap.collector.model.PerformanceMeasurementSession;
import de.isys.jawap.collector.model.PeriodicPerformanceData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class DefaultPerformanceMeasuringService implements PerformanceMeasuringService {

	private static final Log logger = LogFactory.getLog(DefaultPerformanceMeasuringService.class);

	private PerformanceMeasuringDao performanceMeasuringDao;

	@Override
	public void save(PerformanceMeasurementSession performanceMeasurementSession) {
		performanceMeasuringDao.save(performanceMeasurementSession);
	}

	@Override
	public void save(HttpRequestContext requestStats) {
		performanceMeasuringDao.save(requestStats);
	}

	@Override
	public void logStats(HttpRequestContext requestStats) {
		if (logger.isInfoEnabled()) {
			long start = System.currentTimeMillis();
			StringBuilder log = new StringBuilder(10000);
			log.append("\n########## PerformanceStats ##########\n");
			log.append("id: ").append(requestStats.getId()).append('\n');
			log.append(getRequestMessage(requestStats));

			logMethodCallStats(requestStats, log);

			log.append("Printing stats took ").append(System.currentTimeMillis() - start).append(" ms\n");
			log.append("######################################\n\n\n");

			// One log should occur after another like log1 log2, not lolog2g1
			synchronized (logger) {
				logger.info(log.toString());
			}
		}
	}

	private String getRequestMessage(HttpRequestContext requestStats) {
		String requestMessage = "Request to " + requestStats.getUrl();
		if (requestStats.getQueryParams() != null) {
			requestMessage += "?" + requestStats.getQueryParams();
		}
		requestMessage += " took " + requestStats.getExecutionTime() + " ms\n";
		return requestMessage;
	}

	private void logMethodCallStats(HttpRequestContext requestStats, StringBuilder log) {
		if (requestStats.getMethodCallStats() != null) {
			log.append("──────────────────────────────────────────────────\n");
			log.append("Selftime (ms)    Total (ms)       Method signature\n");
			log.append("──────────────────────────────────────────────────\n");

			logStats(requestStats.getMethodCallStats(), requestStats.getMethodCallStats().getExecutionTime(), 0, log);
		}
	}

	private void logStats(MethodCallStats methodCallStats, long totalExecutionTimeNs, int depth, StringBuilder log) {
		appendTimesPercentTable(methodCallStats, totalExecutionTimeNs, log);
		appendCallTree(methodCallStats, depth, log);
		preorderTraverseTreeAndComputeDepth(methodCallStats, totalExecutionTimeNs, depth, log);
	}

	private void appendTimesPercentTable(MethodCallStats methodCallStats, long totalExecutionTimeNs, StringBuilder sb) {
		appendNumber(sb, methodCallStats.getNetExecutionTime());
		appendPercent(sb, methodCallStats.getNetExecutionTime(), totalExecutionTimeNs);

		appendNumber(sb, methodCallStats.getExecutionTime());
		appendPercent(sb, methodCallStats.getExecutionTime(), totalExecutionTimeNs);
	}

	private void appendNumber(StringBuilder sb, long time) {
		sb.append(String.format("%,9.2f", time / 1000000.0)).append("  ");
	}

	private void appendPercent(StringBuilder sb, long time, long totalExecutionTimeNs) {
		sb.append(String.format("%3.0f", time * 100 / (double) totalExecutionTimeNs)).append("%  ");
	}

	private void appendCallTree(MethodCallStats methodCallStats, int depth, StringBuilder sb) {
		for (int i = 1; i <= depth; i++) {
			if (i == depth) {
				if (isLastChild(methodCallStats) && methodCallStats.getChildren().isEmpty()) {
					sb.append("└─ ");
				} else {
					sb.append("├─ ");
				}
			} else {
				sb.append("│  ");
			}
		}
		sb.append(methodCallStats.getSignature()).append('\n');
	}

	private boolean isLastChild(MethodCallStats methodCallStats) {
		final List<MethodCallStats> parentChildren = methodCallStats.parent.getChildren();
		return parentChildren.get(parentChildren.size() - 1) == methodCallStats;
	}

	private void preorderTraverseTreeAndComputeDepth(MethodCallStats methodCallStats, long totalExecutionTimeNs,
													 int depth, StringBuilder log) {
		for (MethodCallStats callStats : methodCallStats.getChildren()) {
			depth++;
			logStats(callStats, totalExecutionTimeNs, depth, log);
			depth--;
		}
	}

	@Override
	public void update(PerformanceMeasurementSession performanceMeasurementSession) {
		performanceMeasuringDao.update(performanceMeasurementSession);
	}

	@Override
	public void save(PeriodicPerformanceData periodicPerformanceData) {
		performanceMeasuringDao.save(periodicPerformanceData);
	}
}
