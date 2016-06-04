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
						// A regex of metric paths. A table row is created for each distinct regex group. That's
						// why the wildcard [^\.]+ is put in parentesis. That way a row is created for each individual
						// cache name
						metricPathRegex: /cache_hit_ratio.([^\.]+).All/,
						metricMatcher: {
							name: "cache_hit_ratio",
							tier: "All"
						},
						groupBy: "cache_name",
						metric: "value",
						// the column label
						title: "Hit Rate (%)"
					},
					{
						metricPathRegex: /cache_size_bytes.([^\.]+).All/,
						metricMatcher: {
							name: "cache_size_bytes",
							tier: "All"
						},
						groupBy: "cache_name",
						metric: "value",
						title: "Bytes used"
					},
					{
						metricPathRegex: /cache_size_count.([^\.]+).All/,
						metricMatcher: {
							name: "cache_size_count",
							tier: "All"
						},
						groupBy: "cache_name",
						metric: "value",
						title: "Elements in cache"
					},
					{
						metricPathRegex: /cache_get.([^\.]+).All/,
						metricMatcher: {
							name: "cache_get",
							tier: "All"
						},
						groupBy: "cache_name",
						metric: "m1_rate",
						title: "Gets/sec"
					},
					{
						metricPathRegex: /cache_get.([^\.]+).All/,
						metricMatcher: {
							name: "cache_get",
							tier: "All"
						},
						groupBy: "cache_name",
						metric: "mean",
						title: "Avg get time"
					},
					{
						metricPathRegex: /cache_get.([^\.]+).All/,
						metricMatcher: {
							name: "cache_get",
							tier: "All"
						},
						groupBy: "cache_name",
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
					defaultRowSelection: '*',
					templates: [
						{
							template: {
								// jQuery expression of a element in the plugin html file the graph should be bound to
								bindto: '#ehcache-hitrate',
								// the minimal value of the y-axis in the graph
								min: 0,
								// the maximum value of the y-axis in the graph
								max: 100,
								// the format of the values
								// one of percent|bytes|ms
								// or some arbitrary string that is appended to the values e.g. 'requests/sec'
								format: 'percent',
								// the amount of colouring of the area between the graph and the x-axis
								fill: 0.1,
								columns: [
									{
										// A regex of metric paths. A line is created for each distinct regex group.
										// That's why the placeholder ${rowName} is put in parentesis.
										// That way a line is created and named after the selected cache name
										metricPathRegex: "cache_hit_(ratio).${rowName}.All",
										metricMatcher: {
											name: "cache_hit_ratio",
											tier: "All",
											cache_name: "${rowName}"
										},
										groupBy: "cache_name",
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
										metricPathRegex: "cache_size_(bytes).${rowName}.All",
										metricMatcher: {
											name: "cache_size_bytes",
											tier: "All",
											cache_name: "${rowName}"
										},
										groupBy: "cache_name",
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

