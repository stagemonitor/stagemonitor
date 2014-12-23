(function () {
	plugins.push(
		{
			id: "jvm-metrics",
			label: "JVM",
			graphs: [
				{
					bindto: '#memory',
					min: 0,
					format: 'bytes',
					fill: 0.1,
					columns: [
						{ metricCategory: "gauges", metricPathRegex: "jvm.memory.heap.(max)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "jvm.memory.heap.(committed)", metric: "value" },
						{ metricCategory: "gauges", metricPathRegex: "jvm.memory.heap.(used)", metric: "value" }
					]
				},
				{
					bindto: '#memory-pools',
					min: 0,
					max: 1,
					format: 'percent',
					columns: [
						{ metricCategory: "gauges", metricPathRegex: /jvm.memory.pools.([^\.]+).usage/, metric: "value" }
					],
					disabledLines: ["Code-Cache"]
				},
				{
					bindto: '#cpu',
					min: 0,
					max: 1,
					fill: 0.1,
					format: 'percent',
					columns: [
						{ metricCategory: "gauges", metricPathRegex: "jvm.cpu.process.(usage)", metric: "value" }
					],
					padding: { bottom: 0, top: 0 }
				},
				{
					bindto: '#gc',
					min: 0,
					fill: 0.1,
					format: 'ms',
					derivative: true,
					columns: [
						{ metricCategory: "gauges", metricPathRegex: /jvm.gc.([^\.]+).time/, metric: "value" }
					],
					padding: { bottom: 0, top: 0 }
				}
			]
		}
	)
}());

