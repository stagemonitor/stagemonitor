package org.stagemonitor.jdbc;

import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.common.ResultSetInformation;
import com.p6spy.engine.common.StatementInformation;
import com.p6spy.engine.event.SimpleJdbcEventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.ExternalRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class StagemonitorJdbcEventListener extends SimpleJdbcEventListener {

	private static final Logger logger = LoggerFactory.getLogger(StagemonitorJdbcEventListener.class);

	private final JdbcPlugin jdbcPlugin;

	private final RequestMonitor requestMonitor;

	private final MetricName.MetricNameTemplate getConnectionTemplate = name("get_jdbc_connection").templateFor("url");

	private final ConcurrentMap<DataSource, String> dataSourceUrlMap = new ConcurrentHashMap<DataSource, String>();

	private CorePlugin corePlugin;

	public StagemonitorJdbcEventListener() {
		this(Stagemonitor.getConfiguration());
	}

	public StagemonitorJdbcEventListener(Configuration configuration) {
		this.jdbcPlugin = configuration.getConfig(JdbcPlugin.class);
		requestMonitor = configuration.getConfig(RequestMonitorPlugin.class).getRequestMonitor();
		corePlugin = configuration.getConfig(CorePlugin.class);
	}

	@Override
	public void onConnectionWrapped(ConnectionInformation connectionInformation) {
		final Metric2Registry metricRegistry = corePlugin.getMetricRegistry();
		if (connectionInformation.getDataSource() instanceof DataSource && metricRegistry != null) {
			DataSource dataSource = (DataSource) connectionInformation.getDataSource();
			ensureUrlExistsForDataSource(dataSource, connectionInformation.getConnection());
			String url = dataSourceUrlMap.get(dataSource);
			metricRegistry.timer(getConnectionTemplate.build(url)).update(connectionInformation.getTimeToGetConnectionNs(), TimeUnit.NANOSECONDS);
		}
	}

	private DataSource ensureUrlExistsForDataSource(DataSource dataSource, Connection connection) {
		if (!dataSourceUrlMap.containsKey(dataSource)) {
			final DatabaseMetaData metaData;
			try {
				metaData = connection.getMetaData();
				dataSourceUrlMap.put(dataSource, metaData.getUserName() + '@' + metaData.getURL());
			} catch (SQLException e) {
				logger.warn(e.getMessage(), e);
			}
		}
		return dataSource;
	}

	@Override
	public void onAfterAnyExecute(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
		final RequestTrace requestTrace = RequestMonitor.get().getRequestTrace();
		if (requestTrace != null && jdbcPlugin.isCollectSql()) {
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
