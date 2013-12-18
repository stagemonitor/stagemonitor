package de.isys.jawap.collector.profiler;

import de.isys.jawap.entities.profiler.CallStackElement;
import de.isys.jawap.entities.profiler.ExecutionContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ExecutionContextLogger {

	private static final Log logger = LogFactory.getLog(ExecutionContextLogger.class);

	private ExecutorService asyncLoggPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			thread.setName("async-request-logger");
			return thread;
		}
	});

	public void logStats(final ExecutionContext requestStats) {
		if (logger.isInfoEnabled()) {
			asyncLoggPool.execute(new Runnable() {
				@Override
				public void run() {
					long start = System.currentTimeMillis();
					StringBuilder log = new StringBuilder(10000);
					log.append("\n########## PerformanceStats ##########\n");
					log.append(requestStats.toString());

					logMethodCallStats(requestStats, log);

					log.append("Printing stats took ").append(System.currentTimeMillis() - start).append(" ms\n");
					log.append("######################################\n\n\n");

					logger.info(log.toString());
				}
			});
		}
	}



	private void logMethodCallStats(ExecutionContext requestStats, StringBuilder log) {
		if (requestStats.getCallStack() != null) {
			log.append("--------------------------------------------------\n");
			log.append("Selftime (ms)    Total (ms)       Method signature\n");
			log.append("--------------------------------------------------\n");

			logStats(requestStats.getCallStack(), requestStats.getCallStack().getExecutionTime(), 0, log);
		}
	}

	private void logStats(CallStackElement callStackElement, long totalExecutionTimeNs, int depth, StringBuilder log) {
		appendTimesPercentTable(callStackElement, totalExecutionTimeNs, log);
		appendCallTree(callStackElement, depth, log);
		preorderTraverseTreeAndComputeDepth(callStackElement, totalExecutionTimeNs, depth, log);
	}

	private void appendTimesPercentTable(CallStackElement callStackElement, long totalExecutionTimeNs, StringBuilder sb) {
		appendNumber(sb, callStackElement.getNetExecutionTime());
		appendPercent(sb, callStackElement.getNetExecutionTime(), totalExecutionTimeNs);

		appendNumber(sb, callStackElement.getExecutionTime());
		appendPercent(sb, callStackElement.getExecutionTime(), totalExecutionTimeNs);
	}

	private void appendNumber(StringBuilder sb, long time) {
		sb.append(String.format("%,9.2f", time / 1000000.0)).append("  ");
	}

	private void appendPercent(StringBuilder sb, long time, long totalExecutionTimeNs) {
		sb.append(String.format("%3.0f", time * 100 / (double) totalExecutionTimeNs)).append("%  ");
	}

	private void appendCallTree(CallStackElement callStackElement, int depth, StringBuilder sb) {
		for (int i = 1; i <= depth; i++) {
			if (i == depth) {
				if (isLastChild(callStackElement) && callStackElement.getChildren().isEmpty()) {
					sb.append("`-- ");
				} else {
					sb.append("+-- ");
				}
			} else {
				sb.append("|   ");
			}
		}
		sb.append(callStackElement.getSignature()).append('\n');
	}

	private boolean isLastChild(CallStackElement callStackElement) {
		final List<CallStackElement> parentChildren = callStackElement.getParent().getChildren();
		return parentChildren.get(parentChildren.size() - 1) == callStackElement;
	}

	private void preorderTraverseTreeAndComputeDepth(CallStackElement callStackElement, long totalExecutionTimeNs,
													 int depth, StringBuilder log) {
		for (CallStackElement callStats : callStackElement.getChildren()) {
			depth++;
			logStats(callStats, totalExecutionTimeNs, depth, log);
			depth--;
		}
	}

}
