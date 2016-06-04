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
						metricPathRegex: /external_request_response_time.jdbc.([^\.]+).+/,
						metricMatcher: {
							name: "external_request_response_time",
							type: "jdbc"
						},
						groupBy: "signature",
						metric: "m1_rate",
						title: "Requests/s"
					},
					{
						metricPathRegex: /external_request_response_time.jdbc.([^\.]+).+/,
						metricMatcher: {
							name: "external_request_response_time",
							type: "jdbc"
						},
						groupBy: "signature",
						metric: "max",
						title: "Max"
					},
					{
						metricPathRegex: /external_request_response_time.jdbc.([^\.]+).+/,
						metricMatcher: {
							name: "external_request_response_time",
							type: "jdbc"
						},
						groupBy: "signature",
						metric: "mean",
						title: "Mean"
					},
					{
						metricPathRegex: /external_request_response_time.jdbc.([^\.]+).+/,
						metricMatcher: {
							name: "external_request_response_time",
							type: "jdbc"
						},
						groupBy: "signature",
						metric: "min",
						title: "Min"
					},
					{
						metricPathRegex: /external_request_response_time.jdbc.([^\.]+).+/,
						metricMatcher: {
							name: "external_request_response_time",
							type: "jdbc"
						},
						groupBy: "signature",
						metric: "p50",
						title: "p50"
					},
					{
						metricPathRegex: /external_request_response_time.jdbc.([^\.]+).+/,
						metricMatcher: {
							name: "external_request_response_time",
							type: "jdbc"
						},
						groupBy: "signature",
						metric: "p95",
						title: "p95"
					},
					{
						metricPathRegex: /external_request_response_time.jdbc.([^\.]+).+/,
						metricMatcher: {
							name: "external_request_response_time",
							type: "jdbc"
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
										metricPathRegex: "external_request_response_time.jdbc.(${rowName})",
										metricMatcher: {
											name: "external_request_response_time",
											type: "jdbc",
											signature: "${rowName}"
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
										metricPathRegex: "external_request_response_time.jdbc.(${rowName})",
										metricMatcher: {
											name: "external_request_response_time",
											type: "jdbc",
											signature: "${rowName}"
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

