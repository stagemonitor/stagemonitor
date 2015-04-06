package org.stagemonitor.jdbc;

import java.util.Collection;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.configuration.converter.SetValueConverter;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

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
					"not listed here, just add it to the list. " +
					"This option can only be set via properties files, environment variables and system properties.")
			.defaultValue(SetValueConverter.immutableSet(
					"org.apache.tomcat.jdbc.pool.DataSource",
					"org.apache.tomcat.dbcp.dbcp.PoolingDataSource",
					"org.apache.commons.dbcp2.PoolingDataSource",
					"org.apache.commons.dbcp.PoolingDataSource",
					"com.mchange.v2.c3p0.PooledDataSource",
					"com.jolbox.bonecp.BoneCPDataSource",
					"snaq.db.DBPoolDataSource",
					"com.zaxxer.hikari.HikariDataSource"))
			.configurationCategory(JDBC_PLUGIN)
			.build();

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration config) {
		ElasticsearchClient elasticsearchClient = config.getConfig(CorePlugin.class).getElasticsearchClient();
		elasticsearchClient.sendGrafanaDashboardAsync("DB Queries.json");
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
