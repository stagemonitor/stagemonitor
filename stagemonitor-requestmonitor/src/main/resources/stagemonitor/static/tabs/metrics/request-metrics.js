(function () {
	plugins.push(
		{
			id: "request-metrics",
			label: "Requests",
			table: {
				bindto: "#request-table",
				nameLabel: "Name",
				columns: [
					{
						metricCategory: "timers",
						metricPathRegex: /response_time_server.([^\.]+).All/,
						metric: "m1_rate",
						title: "Requests/s"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /response_time_server.([^\.]+).All/,
						metric: "max",
						title: "Max"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /response_time_server.([^\.]+).All/,
						metric: "mean",
						title: "Mean"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /response_time_server.([^\.]+).All/,
						metric: "min",
						title: "Min"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /response_time_server.([^\.]+).All/,
						metric: "p50",
						title: "p50"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /response_time_server.([^\.]+).All/,
						metric: "p95",
						title: "p95"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /response_time_server.([^\.]+).All/,
						metric: "stddev",
						title: "Std. Dev."
					},
					{
						metricCategory: "meters",
						metricPathRegex: /external_requests_rate.([^\.]+).jdbc/,
						metric: "m1_rate",
						title: "SQLs/sec"
					}
				],
				graphTemplates: {
					defaultRowSelection: 'All',
					templates: [
						{
							template: {
								bindto: '#time',
								min: 0,
								format: 'ms',
								fill: 0.1,
								columns: [
									{ metricCategory: "timers", metricPathRegex: "response_time_server.(${rowName}).All", metric: "mean" }
								]
							}
						},
						{
							template: {
								bindto: '#throughput',
								min: 0,
								format: 'requests/sec',
								fill: 0.1,
								columns: [
									{ metricCategory: "timers", metricPathRegex: "response_time_server.(${rowName}).All", metric: "m1_rate"}
								]
							}
						}
					]
				}
			}
		}
	);
}());

