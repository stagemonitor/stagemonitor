(function () {
	plugins.push(
		{
			id: "jdbc-metrics",
			label: "JDBC",
			table: {
				bindto: "#jdbc-table",
				nameLabel: "Name",
				columns: [
					{
						metricCategory: "timers",
						metricPathRegex: /jdbc.(.+)/,
						metric: "m1_rate",
						title: "Requests/s"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /jdbc.(.+)/,
						metric: "max",
						title: "Max"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /jdbc.(.+)/,
						metric: "mean",
						title: "Mean"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /jdbc.(.+)/,
						metric: "min",
						title: "Min"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /jdbc.(.+)/,
						metric: "p50",
						title: "p50"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /jdbc.(.+)/,
						metric: "p95",
						title: "p95"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /jdbc.(.+)/,
						metric: "stddev",
						title: "Std. Dev."
					}
				],
				graphTemplates: {
					defaultRowSelection: 'All',
					templates: [
						{
							template: {
								bindto: '#jdbc-time',
								min: 0,
								format: 'ms',
								fill: 0.1,
								columns: [
									{ metricCategory: "timers", metricPathRegex: "jdbc.(${rowName})", metric: "mean" }
								]
							}
						},
						{
							template: {
								bindto: '#jdbc-throughput',
								min: 0,
								format: 'requests/sec',
								fill: 0.1,
								columns: [
									{ metricCategory: "timers", metricPathRegex: "jdbc.(${rowName})", metric: "m1_rate"}
								]
							}
						}
					]
				}
			}
		}
	);
}());

