package org.stagemonitor.jdbc.p6spy;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.concurrent.TimeUnit;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.P6Logger;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.jdbc.JdbcPlugin;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;

public class StagemonitorP6Logger implements P6Logger {

	private final JdbcPlugin jdbcPlugin;
	private final Metric2Registry metricRegistry;

	public StagemonitorP6Logger(Configuration configuration, Metric2Registry metricRegistry) {
		this.jdbcPlugin = configuration.getConfig(JdbcPlugin.class);
		this.metricRegistry = metricRegistry;
	}

	@Override
	public void logSQL(int connectionId, String now, long elapsed, Category category, String prepared, String sql) {
		if (StringUtils.isNotEmpty(prepared)) {
			if (StringUtils.isEmpty(sql)) {
				sql = prepared;
			}
			RequestTrace request = RequestMonitor.getRequest();
			if (request != null) {
				request.dbCallCompleted(elapsed);
				trackDbMetrics(elapsed);
				addSqlToCallStack(elapsed, prepared, sql);
			}
		}
	}

	private void trackDbMetrics(long elapsed) {
		CallStackElement currentCall = Profiler.getMethodCallParent();
		metricRegistry.timer(name("jdbc_statement").tag("signature", "All").build()).update(elapsed, TimeUnit.MILLISECONDS);
		if (currentCall != null) {
			String shortSignature = currentCall.getShortSignature();
			if (shortSignature != null) {
				metricRegistry
						.timer(name("jdbc_statement").tag("signature", shortSignature).build())
						.update(elapsed, TimeUnit.MILLISECONDS);
			}
		}
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
