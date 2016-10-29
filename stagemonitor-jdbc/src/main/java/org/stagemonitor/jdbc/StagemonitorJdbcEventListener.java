package org.stagemonitor.jdbc;

import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.common.StatementInformation;
import com.p6spy.engine.event.SimpleJdbcEventListener;
import com.uber.jaeger.context.TracingUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.AbstractExternalRequest;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.profiler.Profiler;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class StagemonitorJdbcEventListener extends SimpleJdbcEventListener {

	private static final Logger logger = LoggerFactory.getLogger(StagemonitorJdbcEventListener.class);

	private final JdbcPlugin jdbcPlugin;

	private final MetricName.MetricNameTemplate getConnectionTemplate = name("get_jdbc_connection").templateFor("url");

	private final ConcurrentMap<DataSource, String> dataSourceUrlMap = new ConcurrentHashMap<DataSource, String>();

	private CorePlugin corePlugin;
	private RequestMonitorPlugin requestMonitorPlugin;

	public StagemonitorJdbcEventListener() {
		this(Stagemonitor.getConfiguration());
	}

	public StagemonitorJdbcEventListener(Configuration configuration) {
		this.jdbcPlugin = configuration.getConfig(JdbcPlugin.class);
		requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
		corePlugin = configuration.getConfig(CorePlugin.class);
	}

	@Override
	public void onConnectionWrapped(ConnectionInformation connectionInformation) {
		final Metric2Registry metricRegistry = corePlugin.getMetricRegistry();
		// at the moment stagemonitor only supports monitoring connections initiated via a DataSource
		if (connectionInformation.getDataSource() instanceof DataSource && corePlugin.isInitialized()) {
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
	public void onBeforeAnyExecute(StatementInformation statementInformation) {
		requestMonitorPlugin.getRequestMonitor().monitorStart(new MonitoredJdbcRequest(requestMonitorPlugin));
	}

	@Override
	public void onAfterAnyExecute(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
		if (!TracingUtils.getTraceContext().isEmpty()) {
			final Span span = TracingUtils.getTraceContext().getCurrentSpan();
			if (statementInformation.getConnectionInformation().getDataSource() instanceof DataSource && jdbcPlugin.isCollectSql()) {
				String url = dataSourceUrlMap.get(statementInformation.getConnectionInformation().getDataSource());
				Tags.PEER_SERVICE.set(span, url);
				if (StringUtils.isNotEmpty(statementInformation.getSql())) {
					String sql = getSql(statementInformation.getSql(), statementInformation.getSqlWithValues());
					Profiler.addIOCall(sql, timeElapsedNanos);
					span.setTag("method", sql.substring(0, sql.indexOf(' ')).toUpperCase());
					span.setTag("request", sql);
				}

			}
			requestMonitorPlugin.getRequestMonitor().monitorStop();
		}
	}

	private String getSql(String prepared, String sql) {
		if (StringUtils.isEmpty(sql) || !jdbcPlugin.isCollectPreparedStatementParameters()) {
			sql = prepared;
		}
		return sql.trim();
	}

	private static class MonitoredJdbcRequest extends AbstractExternalRequest {

		private MonitoredJdbcRequest(RequestMonitorPlugin requestMonitorPlugin) {
			super(requestMonitorPlugin);
		}

		@Override
		protected String getType() {
			return "jdbc";
		}

	}
}
