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
						metricMatcher: {
							name: "logging"
						},
						groupBy: "log_level",
						metric: "m1_rate",
						title: "Logs/s (1m)"
					},
					{
						metricMatcher: {
							name: "logging"
						},
						groupBy: "log_level",
						metric: "m5_rate",
						title: "Logs/s (5m)"
					},
					{
						metricMatcher: {
							name: "logging"
						},
						groupBy: "log_level",
						metric: "m15_rate",
						title: "Logs/s (15m)"
					},
					{
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
										metricMatcher: {
											name: "logging",
											tags: {
												log_level: "${rowName}"
											}
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
										metricMatcher: {
											name: "logging",
											tags: {
												log_level: "${rowName}"
											}
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

