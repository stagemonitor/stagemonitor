(function () {
	plugins.push(
		{
			id: "os-metrics",
			label: "OS",
			graphs: [
				{
					bindto: '#os-cpu',
					min: 0,
					max: 1,
					format: 'percent',
					stack: true,
					fill: 0.1,
					columns: [
						{ metricCategory: "gauges", metricPathRegex: /os\.cpu\.usage\.((?!idle).*$)/, metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: /os\.cpu\.usage\.(idle)/, metric: "value" }
					]
				},
				{
					bindto: '#network-io',
					min: 0,
					format: 'bytes',
					derivative: true,
					columns: [
						{ metricCategory: "gauges", metricPathRegex: /os.net.[^\.]+.write/, metric: "value", title: "Send" },
						{ metricCategory: "gauges", metricPathRegex: /os.net.[^\.]+.read/, metric: "value", title: "Receive" }
					],
					disabledLines: ["Code-Cache"]
				},
				{
					bindto: '#io',
					min: 0,
					fill: 0.1,
					format: 'bytes',
					derivative: true,
					columns: [
						{ metricCategory: "gauges", metricPathRegex: /os.fs.[^\.]+.(writes).bytes/, metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: /os.fs.[^\.]+.(reads).bytes/, metric: "value" }
					]
				},
				{
					bindto: '#fs-usage',
					min: 0,
					max: 1,
					format: 'percent',
					columns: [
						{ metricCategory: "gauges", metricPathRegex: /os.fs.([^\.]+).usage-percent/, metric: "value" }
					]
				},
				{
					bindto: '#ram',
					min: 0,
					format: 'bytes',
					fill: 0.1,
					columns: [
						{ metricCategory: "gauges", metricPathRegex: "os.mem.usage.(total)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "os.mem.usage.(used)", metric: "value" }
					]
				},
				{
					bindto: '#swap',
					min: 0,
					format: 'bytes',
					fill: 0.1,
					columns: [
						{ metricCategory: "gauges", metricPathRegex: "os.swap.usage.(total)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "os.swap.usage.(used)", metric: "value" }
					]
				}
			]
		}
	)
}());

