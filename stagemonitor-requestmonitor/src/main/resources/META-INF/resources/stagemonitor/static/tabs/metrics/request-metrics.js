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
						title: "Requests/s"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /request.([^\.]+).server.time.total/,
						metric: "max",
						title: "Max"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /request.([^\.]+).server.time.total/,
						metric: "mean",
						title: "Mean"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /request.([^\.]+).server.time.total/,
						metric: "min",
						title: "Min"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /request.([^\.]+).server.time.total/,
						metric: "p50",
						title: "p50"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /request.([^\.]+).server.time.total/,
						metric: "p95",
						title: "p95"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /request.([^\.]+).server.time.total/,
						metric: "stddev",
						title: "Std. Dev."
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

