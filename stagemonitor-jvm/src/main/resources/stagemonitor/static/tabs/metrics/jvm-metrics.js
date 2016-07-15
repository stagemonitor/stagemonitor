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
						{
							metricMatcher: {
								name: "jvm_memory_heap",
								tags: {
									type: "max"
								}
							},
							groupBy: "type",
							metric: "value"
						},
						{
							metricMatcher: {
								name: "jvm_memory_heap",
								tags: {
									type: "committed"
								}
							},
							groupBy: "type",
							metric: "value"
						},
						{
							metricMatcher: {
								name: "jvm_memory_heap",
								tags: {
									type: "used"
								}
							},
							groupBy: "type",
							metric: "value"
						}
					]
				},
				{
					bindto: '#memory-pools',
					min: 0,
					max: 1,
					format: 'percent0To1',
					columns: [
						{
							metricMatcher: {
								name: "jvm_memory_pools",
								tags: {
									type: "usage"
								}
							},
							groupBy: "memory_pool",
							metric: "value"
						}
					]
				},
				{
					bindto: '#cpu',
					min: 0,
					max: 100,
					fill: 0.1,
					format: 'percent',
					columns: [
						{
							metricMatcher: {
								name: "jvm_process_cpu_usage"
							},
							metric: "value",
							title: "usage"
						}
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
							// A regex of metric paths. A line is created for each distinct regex group.
							// That's why the wildcard [^\.]+ is put in parentesis.
							// That way a line is created and named after each GC-Algorithm
							metricMatcher: {
								name: "jvm_gc_time"
							},
							groupBy: "collector",
							metric: "value"
						}
					]
				}
			]
		}
	)
}());

