package org.stagemonitor.jdbc;

import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.common.ResultSetInformation;
import com.p6spy.engine.common.StatementInformation;
import com.p6spy.engine.event.SimpleJdbcEventListener;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.ExternalRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;

import java.sql.SQLException;

public class StagemonitorJdbcEventListener extends SimpleJdbcEventListener {

	private final JdbcPlugin jdbcPlugin;
	private final RequestMonitor requestMonitor;

	public StagemonitorJdbcEventListener() {
		this(Stagemonitor.getConfiguration());
	}

	public StagemonitorJdbcEventListener(Configuration configuration) {
		this.jdbcPlugin = configuration.getConfig(JdbcPlugin.class);
		requestMonitor = configuration.getConfig(RequestMonitorPlugin.class).getRequestMonitor();
	}

	@Override
	public void onAfterAnyExecute(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
		final RequestTrace requestTrace = RequestMonitor.get().getRequestTrace();
		if (requestTrace != null) {
			createExternalRequest(statementInformation, requestTrace, timeElapsedNanos, statementInformation.getSql(), statementInformation.getSqlWithValues());
		}
	}

	private String getExternalRequestAttribute(int connectionId) {
		return "jdbc" + connectionId;
	}

	@Override
	public void onAfterResultSetNext(ResultSetInformation resultSetInformation, long timeElapsedNanos, boolean hasNext, SQLException e) {
		updateExternalRequest(resultSetInformation.getConnectionId(), timeElapsedNanos);
	}

	@Override
	public void onAfterCommit(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
		updateExternalRequest(connectionInformation.getConnectionId(), timeElapsedNanos);
	}

	private void createExternalRequest(StatementInformation statementInformation, RequestTrace requestTrace, long elapsed, String prepared, String sql) {
		if (StringUtils.isNotEmpty(prepared)) {
			sql = getSql(prepared, sql);
			String method = sql.substring(0, sql.indexOf(' ')).toUpperCase();
			final ExternalRequest jdbcRequest = new ExternalRequest("jdbc", method, elapsed, sql);
			requestMonitor.trackExternalRequest(jdbcRequest);
			requestTrace.addRequestAttribute(getExternalRequestAttribute(statementInformation.getConnectionId()), jdbcRequest);
		}
	}

	private void updateExternalRequest(int connectionId, long elapsed) {
		final RequestTrace requestTrace = RequestMonitor.get().getRequestTrace();
		if (requestTrace != null) {
			ExternalRequest externalRequest = (ExternalRequest) requestTrace.getRequestAttribute(getExternalRequestAttribute(connectionId));
			if (externalRequest != null) {
				requestTrace.addTimeToExternalRequest(externalRequest, elapsed);
			}
		}
	}

	private String getSql(String prepared, String sql) {
		if (StringUtils.isEmpty(sql) || !jdbcPlugin.isCollectPreparedStatementParameters()) {
			sql = prepared;
		}
		return sql.trim();
	}
}
