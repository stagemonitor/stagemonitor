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
							name: "response_time",
							tags: {
								operation_type: "jdbc"
							}
						},
						groupBy: "operation_name",
						metric: "m1_rate",
						title: "Requests/s"
					},
					{
						metricMatcher: {
							name: "response_time",
							tags: {
								operation_type: "jdbc"
							}
						},
						groupBy: "operation_name",
						metric: "max",
						title: "Max"
					},
					{
						metricMatcher: {
							name: "response_time",
							tags: {
								operation_type: "jdbc"
							}
						},
						groupBy: "operation_name",
						metric: "mean",
						title: "Mean"
					},
					{
						metricMatcher: {
							name: "response_time",
							tags: {
								operation_type: "jdbc"
							}
						},
						groupBy: "operation_name",
						metric: "min",
						title: "Min"
					},
					{
						metricMatcher: {
							name: "response_time",
							tags: {
								operation_type: "jdbc"
							}
						},
						groupBy: "operation_name",
						metric: "p50",
						title: "p50"
					},
					{
						metricMatcher: {
							name: "response_time",
							tags: {
								operation_type: "jdbc"
							}
						},
						groupBy: "operation_name",
						metric: "p95",
						title: "p95"
					},
					{
						metricMatcher: {
							name: "response_time",
							tags: {
								operation_type: "jdbc"
							}
						},
						groupBy: "operation_name",
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
											name: "response_time",
											tags: {
												operation_type: "jdbc",
												operation_name: "${rowName}"
											}
										},
										groupBy: "operation_name",
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
											name: "response_time",
											tags: {
												operation_type: "jdbc",
												operation_name: "${rowName}"
											}
										},
										groupBy: "operation_name",
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

