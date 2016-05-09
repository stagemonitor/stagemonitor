package org.stagemonitor.jdbc.p6spy;

import java.util.concurrent.TimeUnit;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.P6Logger;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.jdbc.JdbcPlugin;
import org.stagemonitor.requestmonitor.ExternalRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;

public class StagemonitorP6Logger implements P6Logger {

	private final JdbcPlugin jdbcPlugin;
	private final RequestMonitor requestMonitor;

	public StagemonitorP6Logger() {
		this(Stagemonitor.getConfiguration());
	}

	public StagemonitorP6Logger(Configuration configuration) {
		this.jdbcPlugin = configuration.getConfig(JdbcPlugin.class);
		requestMonitor = configuration.getConfig(RequestMonitorPlugin.class).getRequestMonitor();
	}

	@Override
	public void logSQL(final int connectionId, String now, long elapsed, Category category, String prepared, String sql) {
		final RequestTrace requestTrace = RequestMonitor.get().getRequestTrace();
		if (requestTrace == null) {
			return;
		}

		final String externalRequestAttribute = "jdbc" + connectionId;
		if (category == Category.STATEMENT) {
			createExternalRequest(externalRequestAttribute, requestTrace, elapsed, prepared, sql);
		} else {
			updateExternalRequest(externalRequestAttribute, requestTrace, elapsed);
		}
	}

	private void createExternalRequest(final String externalRequestAttribute, RequestTrace requestTrace, long elapsed, String prepared, String sql) {
		if (StringUtils.isNotEmpty(prepared)) {
			sql = getSql(prepared, sql);
			String method = sql.substring(0, sql.indexOf(' ')).toUpperCase();
			final ExternalRequest jdbcRequest = new ExternalRequest("jdbc", method, Math.max(TimeUnit.MILLISECONDS.toNanos(elapsed), 1), sql);
			requestMonitor.trackExternalRequest(jdbcRequest);
			requestTrace.addRequestAttribute(externalRequestAttribute, jdbcRequest);
		}
	}

	private void updateExternalRequest(String externalRequestAttribute, RequestTrace requestTrace, long elapsed) {
		ExternalRequest externalRequest = (ExternalRequest) requestTrace.getRequestAttribute(externalRequestAttribute);
		if (externalRequest != null) {
			requestTrace.addTimeToExternalRequest(externalRequest, elapsed);
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
		return Category.STATEMENT.equals(category) || Category.RESULTSET.equals(category) || Category.RESULT.equals(category);
	}
}
