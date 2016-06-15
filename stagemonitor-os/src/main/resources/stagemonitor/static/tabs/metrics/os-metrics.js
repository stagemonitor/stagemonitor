(function () {
	plugins.push(
		{
			id: "os-metrics",
			label: "OS",
			graphs: [
				{
					bindto: '#os-cpu',
					min: 0,
					max: 101,
					format: 'percent',
					stack: true,
					fill: 0.1,
					columns: [
						{
							metricMatcher: {
								name: "cpu_usage",
								tags: {
									type: "soft-interrupt"
								}
							},
							groupBy: "type",
							metric: "value"
						},
						{
							metricMatcher: {
								name: "cpu_usage",
								tags: {
									type: "interrupt"
								}
							},
							groupBy: "type",
							metric: "value"
						},
						{
							metricMatcher: {
								name: "cpu_usage",
								tags: {
									type: "stolen"
								}
							},
							groupBy: "type",
							metric: "value"
						},
						{
							metricMatcher: {
								name: "cpu_usage",
								tags: {
									type: "nice"
								}
							},
							groupBy: "type",
							metric: "value"
						},
						{
							metricMatcher: {
								name: "cpu_usage",
								tags: {
									type: "wait"
								}
							},
							groupBy: "type",
							metric: "value"
						},
						{
							metricMatcher: {
								name: "cpu_usage",
								tags: {
									type: "sys"
								}
							},
							groupBy: "type",
							metric: "value"
						},
						{
							metricMatcher: {
								name: "cpu_usage",
								tags: {
									type: "user"
								}
							},
							groupBy: "type",
							metric: "value"
						},
						{
							metricMatcher: {
								name: "cpu_usage",
								tags: {
									type: "idle"
								}
							},
							groupBy: "type",
							metric: "value"
						}
					]
				},
				{
					bindto: '#network-io',
					min: 0,
					format: 'bytes',
					derivative: true,
					columns: [
						{
							metricMatcher: {
								name: "network_io",
								tags: {
									type: "write",
									unit: "bytes"
								}
							},
							groupBy: "ifname",
							metric: "value",
							title: "send"
						},
						{
							metricMatcher: {
								name: "network_io",
								tags: {
									type: "read",
									unit: "bytes"
								}
							},
							groupBy: "ifname",
							metric: "value",
							title: "receive"
						}
					]
				},
				{
					bindto: '#io',
					min: 0,
					fill: 0.1,
					format: 'bytes',
					derivative: true,
					columns: [
						{
							metricMatcher: {
								name: "disk_io"
							},
							groupBy: "type",
							metric: "value"
						}
					]
				},
				{
					bindto: '#fs-usage',
					min: 0,
					max: 100,
					format: 'percent',
					columns: [
						{
							metricMatcher: {
								name: "disk_usage_percent"
							},
							groupBy: "mountpoint",
							metric: "value"
						}
					]
				},
				{
					bindto: '#ram',
					min: 0,
					format: 'bytes',
					fill: 0.1,
					columns: [
						{
							metricMatcher: {
								name: "mem_usage"
							},
							groupBy: "type",
							metric: "value"
						}
					]
				},
				{
					bindto: '#swap',
					min: 0,
					format: 'bytes',
					fill: 0.1,
					columns: [
						{
							metricMatcher: {
								name: "swap_usage",
								tags: {
									type: "total"
								}
							},
							groupBy: "type",
							metric: "value"
						},
						{
							metricMatcher: {
								name: "swap_usage",
								tags: {
									type: "used"
								}
							},
							groupBy: "type",
							metric: "value"
						}
					]
				}
			]
		}
	)
}());

