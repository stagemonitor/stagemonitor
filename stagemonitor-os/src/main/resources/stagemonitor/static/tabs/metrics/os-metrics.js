(function () {
	plugins.push(
		{
			id: "os-metrics",
			label: "OS",
			graphs: [
				{
					bindto: '#os-cpu',
					min: 0,
					max: 100,
					format: 'percent',
					stack: true,
					fill: 0.1,
					columns: [
						{ metricCategory: "gauges", metricPathRegex: "cpu_usage.(soft-interrupt)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "cpu_usage.(interrupt)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "cpu_usage.(stolen)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "cpu_usage.(nice)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "cpu_usage.(wait)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "cpu_usage.(sys)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "cpu_usage.(user)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "cpu_usage.(idle)", metric: "value" }
					]
				},
				{
					bindto: '#network-io',
					min: 0,
					format: 'bytes',
					derivative: true,
					columns: [
						{ metricCategory: "gauges", metricPathRegex: /network_io.[^\.]+.write.bytes/, metric: "value", title: "send" },
						{ metricCategory: "gauges", metricPathRegex: /network_io.[^\.]+.read.bytes/, metric: "value", title: "receive" }
					]
				},
				{
					bindto: '#io',
					min: 0,
					fill: 0.1,
					format: 'bytes',
					derivative: true,
					columns: [
						{ metricCategory: "gauges", metricPathRegex: /disk_io.[^\.]+.([^\.]+)/, metric: "value" }
					]
				},
				{
					bindto: '#fs-usage',
					min: 0,
					max: 100,
					format: 'percent',
					columns: [
						{ metricCategory: "gauges", metricPathRegex: /disk_usage_percent.([^\.]+)/, metric: "value" }
					]
				},
				{
					bindto: '#ram',
					min: 0,
					format: 'bytes',
					fill: 0.1,
					columns: [
						{ metricCategory: "gauges", metricPathRegex: /mem_usage\.([^\.]+)/, metric: "value" }
					]
				},
				{
					bindto: '#swap',
					min: 0,
					format: 'bytes',
					fill: 0.1,
					columns: [
						{ metricCategory: "gauges", metricPathRegex: "swap_usage.(total)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "swap_usage.(used)", metric: "value" }
					]
				}
			]
		}
	)
}());

