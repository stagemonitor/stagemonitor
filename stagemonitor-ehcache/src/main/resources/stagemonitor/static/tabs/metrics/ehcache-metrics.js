(function () {
	plugins.push(
		{
			// the pluginId
			// has to be the same as the filename of the JS and html file
			// also, the html and the JS file have to be in the same folder
			id: "ehcache-metrics",
			// The label displayed in the side menu of the metrics tab
			label: "EhCache",
			// creates searchable and pageable datatable
			table: {
				// jQuery expression of a element in the plugin html file the table should be bound to
				bindto: "#ehcache-table",
				// the label of the first column
				nameLabel: "Name",
				columns: [
					{
						// the metrics are grouped by the categories gauges, counters, timers, meters and histograms
						metricCategory: "gauges",
						// A regex of metric paths. A table row is created for each distinct regex group. That's
						// why the wildcard [^\.]+ is put in parentesis. That way a row is created for each individual
						// cache name
						metricPathRegex: /cache_hit_ratio.([^\.]+).All/,
						metric: "value",
						// the column label
						title: "Hit Rate (%)"
					},
					{
						metricCategory: "gauges",
						metricPathRegex: /cache_size_bytes.([^\.]+).All/,
						metric: "value",
						title: "Bytes used"
					},
					{
						metricCategory: "gauges",
						metricPathRegex: /cache_size_count.([^\.]+).All/,
						metric: "value",
						title: "Elements in cache"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /cache_get.([^\.]+).All/,
						metric: "m1_rate",
						title: "Gets/sec"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /cache_get.([^\.]+).All/,
						metric: "mean",
						title: "Avg get time"
					},
					{
						metricCategory: "timers",
						metricPathRegex: /cache_get.([^\.]+).All/,
						metric: "p95",
						title: "p95 get time"
					}
				],
				// Optionally graph templates can be defined.
				// Those templates contain the placeholder ${rowName}
				// Each time a row in the table is selected the placeholder gets replaced by the row name (e.g. the cache name)
				graphTemplates: {
					// This is the default value for the placeholder ${rowName} when no row is selected.
					// [^\\.]+ means, per default, show a line for each cache
					defaultRowSelection: '[^\\.]+',
					templates: [
						{
							template: {
								// jQuery expression of a element in the plugin html file the graph should be bound to
								bindto: '#ehcache-hitrate',
								// the minimal value of the y-axis in the graph
								min: 0,
								// the maximum value of the y-axis in the graph
								max: 1,
								// the format of the values
								// one of percent|bytes|ms
								// or some arbitrary string that is appended to the values e.g. 'requests/sec'
								format: 'percent',
								// the amount of colouring of the area between the graph and the x-axis
								fill: 0.1,
								columns: [
									{
										metricCategory: "gauges",
										// A regex of metric paths. A line is created for each distinct regex group.
										// That's why the placeholder ${rowName} is put in parentesis.
										// That way a line is created and named after the selected cache name
										metricPathRegex: "cache_hit_(ratio).${rowName}.All",
										metric: "value",
										aggregate: 'mean'
									}
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
									{
										metricCategory: "gauges",
										metricPathRegex: "cache_size_(bytes).${rowName}.All",
										metric: "value",
										aggregate: 'sum'
									}
								]
							}
						}
					]
				}

			},
			/**
			 * Called after the corresponding html (${plugin-id}.html) got rendered
			 */
			onHtmlInitialized: function() {
			},
			/**
			 * Called each time metrics from the server are received.
			 * This method can be used to register additional (computed) metrics
			 *
			 * @param metrics
			 */
			onMetricsReceived: function(metrics) {
			}
		}
	);
}());

