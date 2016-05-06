package org.stagemonitor.jdbc.p6spy;

import java.util.concurrent.TimeUnit;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.P6Logger;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.jdbc.JdbcPlugin;
import org.stagemonitor.requestmonitor.ExternalRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

public class StagemonitorP6Logger implements P6Logger {

	private final JdbcPlugin jdbcPlugin;
	private final RequestMonitor requestMonitor;

	public StagemonitorP6Logger(Configuration configuration) {
		this.jdbcPlugin = configuration.getConfig(JdbcPlugin.class);
		requestMonitor = configuration.getConfig(RequestMonitorPlugin.class).getRequestMonitor();
	}

	@Override
	public void logSQL(int connectionId, String now, long elapsed, Category category, String prepared, String sql) {
		if (StringUtils.isNotEmpty(prepared)) {
			sql = getSql(prepared, sql);
			String method = sql.substring(0, sql.indexOf(' ')).toUpperCase();
			requestMonitor.trackExternalRequest(new ExternalRequest("jdbc", method, TimeUnit.MILLISECONDS.toNanos(elapsed), sql));
		}
	}

	private String getSql(String prepared, String sql) {
		if (StringUtils.isEmpty(sql) || !jdbcPlugin.isCollectPreparedStatementParameters()) {
			sql = prepared;
		}
		return sql.trim();
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
