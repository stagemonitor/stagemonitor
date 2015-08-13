(function () {
	plugins.push(
		{
			// the pluginId
			// has to be the same as the filename of the JS and html file
			// also, the html and the JS file have to be in the same folder
			id: "jvm-metrics",
			// The label displayed in the side menu of the metrics tab
			label: "JVM",
			// the declaration of the graphs to be created for this plugin
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
					]
				},
				{
					bindto: '#cpu',
					min: 0,
					max: 1,
					fill: 0.1,
					format: 'percent',
					columns: [
						{ metricCategory: "gauges", metricPathRegex: "jvm_process_cpu_(usage)", metric: "value" }
					]
				},
				{
					// jQuery expression of a element in the plugin html file the graph should be bound to
					bindto: '#gc',
					// the minimal value of the y-axis in the graph
					min: 0,
					// the maximum value of the y-axis in the graph
					// max: 1,
					fill: 0.1,
					// the format of the values
					// one of percent|bytes|ms
					// or some arbitrary string that is appended to the values e.g. 'requests/sec'
					format: 'ms',
					// graphs the deltas of the metric
					derivative: true,
					columns: [
						{
							metricCategory: "gauges",
							// A regex of metric paths. A line is created for each distinct regex group.
							// That's why the wildcard [^\.]+ is put in parentesis.
							// That way a line is created and named after each GC-Algorithm
							metricPathRegex: /jvm.gc.([^\.]+).time/,
							metric: "value"
						}
					]
				}
			]
		}
	)
}());

