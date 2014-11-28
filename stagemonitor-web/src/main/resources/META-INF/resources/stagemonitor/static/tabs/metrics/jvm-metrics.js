(function () {
	plugins.push(
		{
			id: "jvm-metrics",
			label: "JVM",
			htmlPath: "tabs/metrics/jvm-metrics.html",
			graphs: [
				{
					bindto: '#memory',
					min: 0,
					format: 'bytes',
					fill: 0.1,
					columns: [
						["gauges", "jvm.memory.heap.(max)", "value"],
						["gauges", "jvm.memory.heap.(committed)", "value"],
						["gauges", "jvm.memory.heap.(used)", "value"]
					]
				},
				{
					bindto: '#memory-pools',
					min: 0,
					max: 1,
					format: 'percent',
					columns: [
						["gauges", /jvm.memory.pools.([^\.]+).usage/, "value"]
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
						["gauges", "jvm.cpu.process.(usage)", "value"]
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
						["gauges", /jvm.gc.([^\.]+).time/, "value"]
					],
					padding: { bottom: 0, top: 0 }
				}
			],
			onMetricsReceived: function (metrics) {

			}
		}
	)
}());

