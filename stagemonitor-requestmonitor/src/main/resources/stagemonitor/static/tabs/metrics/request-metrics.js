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
						metricMatcher: {
							name: "response_time_server",
							tags: {
								layer: "All"
							}
						},
						groupBy: "request_name",
						metric: "m1_rate",
						title: "Requests/s"
					},
					{
						metricMatcher: {
							name: "response_time_server",
							tags: {
								layer: "All"
							}
						},
						groupBy: "request_name",
						metric: "max",
						title: "Max"
					},
					{
						metricMatcher: {
							name: "response_time_server",
							tags: {
								layer: "All"
							}
						},
						groupBy: "request_name",
						metric: "mean",
						title: "Mean"
					},
					{
						metricMatcher: {
							name: "response_time_server",
							tags: {
								layer: "All"
							}
						},
						groupBy: "request_name",
						metric: "min",
						title: "Min"
					},
					{
						metricMatcher: {
							name: "response_time_server",
							tags: {
								layer: "All"
							}
						},
						groupBy: "request_name",
						metric: "p50",
						title: "p50"
					},
					{
						metricMatcher: {
							name: "response_time_server",
							tags: {
								layer: "All"
							}
						},
						groupBy: "request_name",
						metric: "p95",
						title: "p95"
					},
					{
						metricMatcher: {
							name: "response_time_server",
							tags: {
								layer: "All"
							}
						},
						groupBy: "request_name",
						metric: "std",
						title: "Std. Dev."
					},
					{
						metricMatcher: {
							name: "external_requests_rate",
							tags: {
								type: "jdbc"
							}
						},
						groupBy: "request_name",
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
									{
										metricMatcher: {
											name: "response_time_server",
											tags: {
												layer: "All",
												request_name: "${rowName}"
											}
										},
										groupBy: "request_name",
										metric: "mean"
									}
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
									{
										metricMatcher: {
											name: "response_time_server",
											tags: {
												layer: "All",
												request_name: "${rowName}"
											}
										},
										groupBy: "request_name",
										metric: "m1_rate"
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

