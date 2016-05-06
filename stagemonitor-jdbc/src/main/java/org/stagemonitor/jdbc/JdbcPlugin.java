package org.stagemonitor.jdbc;

import java.util.Collection;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.configuration.converter.SetValueConverter;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.grafana.GrafanaClient;

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
					"org.apache.tomcat.jdbc.pool.DataSourceProxy",
					"org.apache.commons.dbcp2.PoolingDataSource",
					"org.apache.commons.dbcp.PoolingDataSource",
					"com.mchange.v2.c3p0.AbstractPoolBackedDataSource",
					"com.mchange.v2.c3p0.PoolBackedDataSource",
					"com.mchange.v2.c3p0.ComboPooledDataSource",
					"com.jolbox.bonecp.BoneCPDataSource",
					"snaq.db.DBPoolDataSource",
					"com.zaxxer.hikari.HikariDataSource",
					"org.jboss.jca.adapters.jdbc.WrapperDataSource",
					"org.springframework.jdbc.datasource.AbstractDriverBasedDataSource"
			))
			.configurationCategory(JDBC_PLUGIN)
			.build();

	@Override
	public void initializePlugin(StagemonitorPlugin.InitArguments initArguments) {
		final CorePlugin corePlugin = initArguments.getPlugin(CorePlugin.class);
		ElasticsearchClient elasticsearchClient = corePlugin.getElasticsearchClient();
		final GrafanaClient grafanaClient = corePlugin.getGrafanaClient();
		if (corePlugin.isReportToGraphite()) {
			elasticsearchClient.sendGrafana1DashboardAsync("grafana/Grafana1GraphiteDBQueries.json");
		}
		if (corePlugin.isReportToElasticsearch()) {
			elasticsearchClient.sendClassPathRessourceBulkAsync("kibana/JdbcDashboard.bulk");
			grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchJdbcDashboard.json");
		}
	}

	@Override
	public void registerWidgetMetricTabPlugins(WidgetMetricTabPluginsRegistry widgetMetricTabPluginsRegistry) {
		widgetMetricTabPluginsRegistry.addWidgetMetricTabPlugin("/stagemonitor/static/tabs/metrics/jdbc-metrics");
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
