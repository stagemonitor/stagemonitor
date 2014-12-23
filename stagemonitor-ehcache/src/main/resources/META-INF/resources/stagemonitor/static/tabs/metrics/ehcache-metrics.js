(function () {
	plugins.push(
		{
			id: "ehcache-metrics",
			label: "EhCache",
			table: {
				bindto: "#ehcache-table",
				nameLabel: "Name",
				columns: [
					{
						metricCategory: "gauges",
						metricPathRegex: /cache.([^\.]+).access.hit.total.ratio/,
						metric: "value",
						title: "Hit Rate (%)",
						uniqueName: "hitrate"
					},
					{
						metricCategory: "gauges",
						metricPathRegex: /cache.([^\.]+).bytes.used/,
						metric: "value",
						title: "Bytes used",
						uniqueName: "bytes"
					},
					{
						metricCategory: "gauges",
						metricPathRegex: /cache.([^\.]+).size.count/,
						metric: "value",
						title: "Elements in cache",
						uniqueName: "elements"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /cache.([^\.]+).get/,
						metric: "m1_rate",
						title: "Gets/sec",
						uniqueName: "gps"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /cache.([^\.]+).get/,
						metric: "mean",
						title: "Avg get time",
						uniqueName: "avg"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /cache.([^\.]+).get/,
						metric: "p95",
						title: "p95 get time",
						uniqueName: "p95"
					}
				],
				graphTemplates: {
					// per default, show a line for each cache
					defaultRowSelection: '([^\\.]+)',
					templates: [
						{
							template: {
								bindto: '#ehcache-hitrate',
								min: 0,
								max: 1,
								format: 'percent',
								fill: 0.1,
								columns: [
									{ metricCategory: "gauges", metricPathRegex: "cache.(${rowName}).access.hit.total.ratio", metric: "value" }
								]
							}
						},
						{
							template: {
								bindto: '#ehcache-size',
								min: 0,
								format: 'bytes',
								fill: 0.1,
								columns: [
									{ metricCategory: "gauges", metricPathRegex: "cache.(${rowName}).bytes.used", metric: "value" }
								]
							}
						}
					]
				}

			}
		}
	);
}());

