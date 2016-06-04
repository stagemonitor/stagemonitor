(function () {
	plugins.push(
		{
			id: "logging-metrics",
			label: "Logging",
			table: {
				bindto: "#logging-table",
				nameLabel: "Name",
				columns: [
					{
						metricPathRegex: /logging.([^\.]+)/,
						metricMatcher: {
							name: "logging"
						},
						groupBy: "log_level",
						metric: "m1_rate",
						title: "Logs/s (1m)"
					},
					{
						metricPathRegex: /logging.([^\.]+)/,
						metricMatcher: {
							name: "logging"
						},
						groupBy: "log_level",
						metric: "m5_rate",
						title: "Logs/s (5m)"
					},
					{
						metricPathRegex: /logging.([^\.]+)/,
						metricMatcher: {
							name: "logging"
						},
						groupBy: "log_level",
						metric: "m15_rate",
						title: "Logs/s (15m)"
					},
					{
						metricPathRegex: /logging.([^\.]+)/,
						metricMatcher: {
							name: "logging"
						},
						groupBy: "log_level",
						metric: "count",
						title: "Count"
					}
				],
				graphTemplates: {
					defaultRowSelection: '*',
					templates: [
						{
							template: {
								bindto: '#logging-m1-rate',
								min: 0,
								format: 'logs/sec',
								fill: 0.1,
								columns: [
									{
										metricPathRegex: "logging.(${rowName})",
										metricMatcher: {
											name: "logging",
											log_level: "${rowName}"
										},
										groupBy: "log_level",
										metric: "m1_rate" 
									}
								]
							}
						},
						{
							template: {
								bindto: '#logging-m5-rate',
								min: 0,
								format: 'logs/sec',
								fill: 0.1,
								columns: [
									{
										metricPathRegex: "logging.(${rowName})",
										metricMatcher: {
											name: "logging",
											log_level: "${rowName}"
										},
										groupBy: "log_level",
										metric: "m5_rate"
									}
								]
							}
						}
					]
				}
			}
		}
	);
}());

