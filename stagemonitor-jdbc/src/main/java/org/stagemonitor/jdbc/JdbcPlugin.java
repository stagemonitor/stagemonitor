package org.stagemonitor.jdbc;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.ConfigurationOption;
import org.stagemonitor.core.StageMonitorPlugin;
import org.stagemonitor.core.rest.RestClient;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

import java.util.ArrayList;
import java.util.List;

public class JdbcPlugin implements StageMonitorPlugin {
	public static final String COLLECT_SQL = "stagemonitor.profiler.jdbc.collectSql";
	public static final String COLLECT_PREPARED_STATEMENT_PARAMETERS = "stagemonitor.profiler.jdbc.collectPreparedStatementParameters";

	@Override
	public List<ConfigurationOption> getConfigurationOptions() {
		List<ConfigurationOption> config = new ArrayList<ConfigurationOption>();
		config.add(ConfigurationOption.builder()
				.key(COLLECT_SQL)
				.dynamic(false)
				.label("Collect SQLs in call tree")
				.description("Whether or not sql statements should be included in the call stack.")
				.defaultValue("true")
				.build());
		config.add(ConfigurationOption.builder()
				.key(COLLECT_PREPARED_STATEMENT_PARAMETERS)
				.dynamic(true)
				.label("Collect prepared statement parameters")
				.description("Whether or not the prepared statement placeholders (?) should be replaced with the actual parameters.")
				.defaultValue("false")
				.build());
		config.add(ConfigurationOption.builder()
				.key(RequestMonitorPlugin.COLLECT_DB_TIME_PER_REQUEST)
				.dynamic(true)
				.label("Collect db time per request group")
				.description("Whether or not db execution time should be collected per request group\n" +
						"If set to true, a timer will be created for each request to record the total db time per request.")
				.defaultValue("false")
				.build());
		return config;
	}

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration config) {
		RestClient.sendGrafanaDashboardAsync(config.getElasticsearchUrl(), "DB Queries.json");
	}
}
