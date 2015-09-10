package org.stagemonitor.jdbc;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.configuration.converter.SetValueConverter;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.IOUtils;

public class JdbcPlugin extends StagemonitorPlugin {
	public static final String JDBC_PLUGIN = "JDBC Plugin";
	private final ConfigurationOption<Boolean> collectSql = ConfigurationOption.booleanOption()
			.key("stagemonitor.profiler.jdbc.collectSql")
			.dynamic(false)
			.label("Collect SQLs in call tree")
			.description("Whether or not sql statements should be included in the call stack.")
			.defaultValue(true)
			.configurationCategory(JDBC_PLUGIN)
			.build();
	private final ConfigurationOption<Boolean> collectPreparedStatementParameters = ConfigurationOption.booleanOption()
			.key("stagemonitor.profiler.jdbc.collectPreparedStatementParameters")
			.dynamic(true)
			.label("Collect prepared statement parameters")
			.description("Whether or not the prepared statement placeholders (?) should be replaced with the actual parameters.")
			.defaultValue(true)
			.tags("security-relevant")
			.configurationCategory(JDBC_PLUGIN)
			.build();
	private final ConfigurationOption<Collection<String>> dataSourceImplementations = ConfigurationOption.stringsOption()
			.key("stagemonitor.instrument.jdbc.dataSource.implementations")
			.dynamic(false)
			.label("Class name of DataSource implementations")
			.description("The class name of all known javax.sql.DataSource implementations. If your favourite implementation is " +
					"not listed here, just add it to the list.")
			.defaultValue(SetValueConverter.immutableSet(
					"org.apache.tomcat.jdbc.pool.DataSource",
					"org.apache.tomcat.dbcp.dbcp.PoolingDataSource",
					"org.apache.commons.dbcp2.PoolingDataSource",
					"org.apache.commons.dbcp.PoolingDataSource",
					"com.mchange.v2.c3p0.AbstractPoolBackedDataSource",
					"com.mchange.v2.c3p0.PoolBackedDataSource",
					"com.mchange.v2.c3p0.ComboPooledDataSource",
					"com.jolbox.bonecp.BoneCPDataSource",
					"snaq.db.DBPoolDataSource",
					"com.zaxxer.hikari.HikariDataSource",
					"org.jboss.jca.adapters.jdbc.WrapperDataSource",
					"org.springframework.jdbc.datasource.SingleConnectionDataSource",
					"org.springframework.jdbc.datasource.DriverManagerDataSource",
					"org.springframework.jdbc.datasource.SimpleDriverDataSource"
			))
			.configurationCategory(JDBC_PLUGIN)
			.build();

	@Override
	public void initializePlugin(Metric2Registry metricRegistry, Configuration config) {
		final CorePlugin corePlugin = config.getConfig(CorePlugin.class);
		ElasticsearchClient elasticsearchClient = corePlugin.getElasticsearchClient();
		if (corePlugin.isReportToGraphite()) {
			elasticsearchClient.sendGrafana1DashboardAsync("grafana/Grafana1GraphiteDBQueries.json");
		}
		if (corePlugin.isReportToElasticsearch()) {
			elasticsearchClient.sendBulkAsync(IOUtils.getResourceAsStream("JdbcDashboard.bulk"));
		}
	}

	@Override
	public List<String> getPathsOfWidgetMetricTabPlugins() {
		return Collections.singletonList("/stagemonitor/static/tabs/metrics/jdbc-metrics");
	}

	public boolean isCollectSql() {
		return collectSql.getValue();
	}

	public boolean isCollectPreparedStatementParameters() {
		return collectPreparedStatementParameters.getValue();
	}

	public Collection<String> getDataSourceImplementations() {
		return dataSourceImplementations.getValue();
	}

}
