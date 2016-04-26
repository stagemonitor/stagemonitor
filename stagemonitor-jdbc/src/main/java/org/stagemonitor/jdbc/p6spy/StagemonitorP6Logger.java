package org.stagemonitor.jdbc.p6spy;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.concurrent.TimeUnit;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.P6Logger;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.instrument.StagemonitorClassNameMatcher;
import org.stagemonitor.core.metrics.aspects.SignatureUtils;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.jdbc.JdbcPlugin;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.requestmonitor.profiler.Profiler;

public class StagemonitorP6Logger implements P6Logger {

	private final JdbcPlugin jdbcPlugin;
	private final CorePlugin corePlugin;
	private final Metric2Registry metricRegistry;

	public StagemonitorP6Logger(Configuration configuration, Metric2Registry metricRegistry) {
		this.jdbcPlugin = configuration.getConfig(JdbcPlugin.class);
		this.corePlugin = configuration.getConfig(CorePlugin.class);
		this.metricRegistry = metricRegistry;
	}

	@Override
	public void logSQL(int connectionId, String now, long elapsed, Category category, String prepared, String sql) {
		if (StringUtils.isNotEmpty(prepared)) {
			if (StringUtils.isEmpty(sql)) {
				sql = prepared;
			}
			trackDbMetrics(elapsed);
			RequestTrace request = RequestMonitor.getRequest();
			if (request != null) {
				request.dbCallCompleted(elapsed);
				addSqlToCallStack(elapsed, prepared, sql);
			}
		}
	}

	private void trackDbMetrics(long elapsed) {
		metricRegistry.timer(name("jdbc_statement").tag("signature", "All").build()).update(elapsed, TimeUnit.MILLISECONDS);
		String daoMethodSignature = getDaoMethodSignature();
		if (daoMethodSignature != null) {
			metricRegistry
					.timer(name("jdbc_statement").tag("signature", daoMethodSignature).build())
					.update(elapsed, TimeUnit.MILLISECONDS);
		}
	}

	/**
	 * Returns the signature of the method (inside the monitored codebase) which triggered the execution of the SQL statement.
	 */
	private String getDaoMethodSignature() {
		if (corePlugin.getIncludePackages().isEmpty()) {
			return null;
		}
		String daoMethodSignature = null;
		for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
			if (StagemonitorClassNameMatcher.isIncluded(stackTraceElement.getClassName())) {
				daoMethodSignature = SignatureUtils.getSignature(stackTraceElement.getClassName(), stackTraceElement.getMethodName());
				break;
			}
		}
		return daoMethodSignature;
	}

	private void addSqlToCallStack(long elapsed, String prepared, String sql) {
		if (jdbcPlugin.isCollectPreparedStatementParameters()) {
			Profiler.addIOCall(sql, TimeUnit.MILLISECONDS.toNanos(elapsed));
		} else {
			Profiler.addIOCall(prepared, TimeUnit.MILLISECONDS.toNanos(elapsed));
		}
	}

	@Override
	public void logException(Exception e) {
	}

	@Override
	public void logText(String text) {
	}

	@Override
	public boolean isCategoryEnabled(Category category) {
		return Category.STATEMENT == category;
	}
}
