package org.stagemonitor.jdbc;

import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.converter.SetValueConverter;
import org.stagemonitor.core.StagemonitorPlugin;

import java.util.Collection;

public class JdbcPlugin extends StagemonitorPlugin {
	public static final String JDBC_PLUGIN = "JDBC Plugin";
	private final ConfigurationOption<Boolean> collectSql = ConfigurationOption.booleanOption()
			.key("stagemonitor.profiler.jdbc.collectSql")
			.dynamic(false)
			.label("Collect SQLs in call tree")
			.description("Whether or not sql statements should be included in the call stack.")
			.configurationCategory(JDBC_PLUGIN)
			.buildWithDefault(true);
	private final ConfigurationOption<Boolean> collectPreparedStatementParameters = ConfigurationOption.booleanOption()
			.key("stagemonitor.profiler.jdbc.collectPreparedStatementParameters")
			.dynamic(true)
			.label("Collect prepared statement parameters")
			.description("Whether or not the prepared statement placeholders (?) should be replaced with the actual parameters.")
			.tags("security-relevant")
			.configurationCategory(JDBC_PLUGIN)
			.buildWithDefault(true);
	private final ConfigurationOption<Collection<String>> dataSourceImplementations = ConfigurationOption.stringsOption()
			.key("stagemonitor.instrument.jdbc.dataSource.implementations")
			.dynamic(false)
			.label("Class name of DataSource implementations")
			.description("The class name of all known javax.sql.DataSource implementations. If your favourite implementation is " +
					"not listed here, just add it to the list.")
			.configurationCategory(JDBC_PLUGIN)
			.buildWithDefault(SetValueConverter.immutableSet(
					"org.apache.tomcat.jdbc.pool.DataSource",
					"org.apache.tomcat.dbcp.dbcp.PoolingDataSource",
					"org.apache.tomcat.jdbc.pool.DataSourceProxy",
					"org.apache.commons.dbcp2.PoolingDataSource",
					"org.apache.commons.dbcp.PoolingDataSource",
					"com.mchange.v2.c3p0.impl.AbstractPoolBackedDataSource",
					"com.mchange.v2.c3p0.PoolBackedDataSource",
					"com.mchange.v2.c3p0.ComboPooledDataSource",
					"com.jolbox.bonecp.BoneCPDataSource",
					"snaq.db.DBPoolDataSource",
					"com.zaxxer.hikari.HikariDataSource",
					"org.jboss.jca.adapters.jdbc.WrapperDataSource",
					"org.springframework.jdbc.datasource.AbstractDriverBasedDataSource"
			));

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
