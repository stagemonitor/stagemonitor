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
							metricPathRegex: "cpu_usage.(soft-interrupt)",
							metricMatcher: {
								name: "cpu_usage",
								type: "soft-interrupt"
							},
							groupBy: "type",
							metric: "value"
						},
						{
							metricPathRegex: "cpu_usage.(interrupt)",
							metricMatcher: {
								name: "cpu_usage",
								type: "interrupt"
							},
							groupBy: "type",
							metric: "value"
						},
						{
							metricPathRegex: "cpu_usage.(stolen)",
							metricMatcher: {
								name: "cpu_usage",
								type: "stolen"
							},
							groupBy: "type",
							metric: "value"
						},
						{
							metricPathRegex: "cpu_usage.(nice)",
							metricMatcher: {
								name: "cpu_usage",
								type: "nice"
							},
							groupBy: "type",
							metric: "value"
						},
						{
							metricPathRegex: "cpu_usage.(wait)",
							metricMatcher: {
								name: "cpu_usage",
								type: "wait"
							},
							groupBy: "type",
							metric: "value"
						},
						{
							metricPathRegex: "cpu_usage.(sys)",
							metricMatcher: {
								name: "cpu_usage",
								type: "sys"
							},
							groupBy: "type",
							metric: "value"
						},
						{
							metricPathRegex: "cpu_usage.(user)",
							metricMatcher: {
								name: "cpu_usage",
								type: "user"
							},
							groupBy: "type",
							metric: "value"
						},
						{
							metricPathRegex: "cpu_usage.(idle)",
							metricMatcher: {
								name: "cpu_usage",
								type: "idle"
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
							metricPathRegex: /network_io.[^\.]+.write.bytes/,
							metricMatcher: {
								name: "network_io",
								type: "write",
								unit: "bytes"
							},
							groupBy: "ifname",
							metric: "value",
							title: "send" 
						},
						{
							metricPathRegex: /network_io.[^\.]+.read.bytes/,
							metricMatcher: {
								name: "network_io",
								type: "read",
								unit: "bytes"
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
							metricPathRegex: /disk_io.[^\.]+.([^\.]+)/,
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
							metricPathRegex: /disk_usage_percent.([^\.]+)/,
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
							metricPathRegex: /mem_usage\.([^\.]+)/,
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
							metricPathRegex: "swap_usage.(total)",
							metricMatcher: {
								name: "swap_usage",
								type: "total"
							},
							groupBy: "type",
							metric: "value"
						},
						{
							metricPathRegex: "swap_usage.(used)",
							metricMatcher: {
								name: "swap_usage",
								type: "used"
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

