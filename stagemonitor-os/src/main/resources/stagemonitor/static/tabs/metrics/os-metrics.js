(function () {
	plugins.push(
		{
			id: "os-metrics",
			label: "OS",
			graphs: [
				{
					bindto: '#os-cpu',
					min: 0,
<<<<<<< Updated upstream
					max: 1,
=======
					max: 101,
>>>>>>> Stashed changes
					format: 'percent',
					stack: true,
					fill: 0.1,
					columns: [
						{ metricCategory: "gauges", metricPathRegex: "os.cpu.usage.(soft-interrupt)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "os.cpu.usage.(interrupt)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "os.cpu.usage.(stolen)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "os.cpu.usage.(nice)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "os.cpu.usage.(wait)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "os.cpu.usage.(sys)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "os.cpu.usage.(user)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "os.cpu.usage.(idle)", metric: "value" }
					]
				},
				{
					bindto: '#network-io',
					min: 0,
					format: 'bytes',
					derivative: true,
					columns: [
						{ metricCategory: "gauges", metricPathRegex: /os.net.[^\.]+.write/, metric: "value", title: "send" },
						{ metricCategory: "gauges", metricPathRegex: /os.net.[^\.]+.read/, metric: "value", title: "receive" }
					]
				},
				{
					bindto: '#io',
					min: 0,
					fill: 0.1,
					format: 'bytes',
					derivative: true,
					columns: [
						{ metricCategory: "gauges", metricPathRegex: /os.fs.[^\.]+.([^\.]+).bytes/, metric: "value" }
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
						{ metricCategory: "gauges", metricPathRegex: /os\.mem\.usage\.([^\.]+)/, metric: "value" }
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

