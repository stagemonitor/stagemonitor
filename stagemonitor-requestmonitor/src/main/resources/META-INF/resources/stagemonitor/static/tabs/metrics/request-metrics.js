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
						metricPathRegex: /request.([^\.]+).server.time.total/,
						metric: "m1_rate",
						title: "Requests/s",
						uniqueName: "requests"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /request.([^\.]+).server.time.total/,
						metric: "max",
						title: "Max",
						uniqueName: "max"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /request.([^\.]+).server.time.total/,
						metric: "mean",
						title: "Mean",
						uniqueName: "mean"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /request.([^\.]+).server.time.total/,
						metric: "min",
						title: "Min",
						uniqueName: "min"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /request.([^\.]+).server.time.total/,
						metric: "p50",
						title: "p50",
						uniqueName: "p50"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /request.([^\.]+).server.time.total/,
						metric: "p95",
						title: "p95",
						uniqueName: "p95"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /request.([^\.]+).server.time.total/,
						metric: "stddev",
						title: "Std. Dev.",
						uniqueName: "stddev"
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
									{ metricCategory: "timers", metricPathRegex: "request.(${rowName}).server.time.total", metric: "mean" }
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
									{ metricCategory: "timers", metricPathRegex: "request.(${rowName}).server.time.total", metric: "m1_rate"}
								]
							}
						}
					]
				}
			}
		}
	);
}());

