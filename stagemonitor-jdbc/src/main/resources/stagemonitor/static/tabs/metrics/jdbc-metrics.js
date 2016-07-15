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
						metricMatcher: {
							name: "external_request_response_time",
							tags: {
								type: "jdbc"
							}
						},
						groupBy: "signature",
						metric: "m1_rate",
						title: "Requests/s"
					},
					{
						metricMatcher: {
							name: "external_request_response_time",
							tags: {
								type: "jdbc"
							}
						},
						groupBy: "signature",
						metric: "max",
						title: "Max"
					},
					{
						metricMatcher: {
							name: "external_request_response_time",
							tags: {
								type: "jdbc"
							}
						},
						groupBy: "signature",
						metric: "mean",
						title: "Mean"
					},
					{
						metricMatcher: {
							name: "external_request_response_time",
							tags: {
								type: "jdbc"
							}
						},
						groupBy: "signature",
						metric: "min",
						title: "Min"
					},
					{
						metricMatcher: {
							name: "external_request_response_time",
							tags: {
								type: "jdbc"
							}
						},
						groupBy: "signature",
						metric: "p50",
						title: "p50"
					},
					{
						metricMatcher: {
							name: "external_request_response_time",
							tags: {
								type: "jdbc"
							}
						},
						groupBy: "signature",
						metric: "p95",
						title: "p95"
					},
					{
						metricMatcher: {
							name: "external_request_response_time",
							tags: {
								type: "jdbc"
							}
						},
						groupBy: "signature",
						metric: "std",
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
									{
										metricMatcher: {
											name: "external_request_response_time",
											tags: {
												type: "jdbc",
												signature: "${rowName}"
											}
										},
										groupBy: "signature",
										metric: "mean"
									}
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
									{
										metricMatcher: {
											name: "external_request_response_time",
											tags: {
												type: "jdbc",
												signature: "${rowName}"
											}
										},
										groupBy: "signature",
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

