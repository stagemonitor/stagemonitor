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
						metricCategory: "meters",
						metricPathRegex: /logging.([^\.]+)/,
						metric: "m1_rate",
						title: "Logs/s (1m)"
					},
					{
						metricCategory: "meters",
						metricPathRegex: /logging.([^\.]+)/,
						metric: "m5_rate",
						title: "Logs/s (5m)"
					},
					{
						metricCategory: "meters",
						metricPathRegex: /logging.([^\.]+)/,
						metric: "m15_rate",
						title: "Logs/s (15m)"
					},
					{
						metricCategory: "meters",
						metricPathRegex: /logging.([^\.]+)/,
						metric: "count",
						title: "Count"
					}
				],
				graphTemplates: {
					defaultRowSelection: '[^\\.]+',
					templates: [
						{
							template: {
								bindto: '#logging-m1-rate',
								min: 0,
								format: 'logs/sec',
								fill: 0.1,
								columns: [
									{ metricCategory: "meters", metricPathRegex: "logging.(${rowName})", metric: "m1_rate" }
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
									{ metricCategory: "meters", metricPathRegex: "logging.(${rowName})", metric: "m5_rate"}
								]
							}
						}
					]
				}
			}
		}
	);
}());

