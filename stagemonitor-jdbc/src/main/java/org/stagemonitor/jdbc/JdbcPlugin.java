package org.stagemonitor.jdbc;

import java.util.Arrays;
import java.util.List;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.rest.ElasticsearchClient;

public class JdbcPlugin implements StagemonitorPlugin {
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
			.defaultValue(false)
			.configurationCategory(JDBC_PLUGIN)
			.build();

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		return Arrays.<ConfigurationOption<?>>asList(collectSql, collectPreparedStatementParameters);
	}

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration config) {
		ElasticsearchClient.sendGrafanaDashboardAsync("DB Queries.json");
	}

	public boolean isCollectSql() {
		return collectSql.getValue();
	}

	public boolean isCollectPreparedStatementParameters() {
		return collectPreparedStatementParameters.getValue();
	}
}
