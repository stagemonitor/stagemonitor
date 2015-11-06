var graphRenderer = (function () {
	var selectedPluginId;
	var storedMinutes = +localStorage.getItem("widget-settings-metrics-history");
	var graphs = [];

	return {
		renderGraphs: function (plugins, metrics, onGraphsRendered) {
			initGraphs(plugins, metrics, onGraphsRendered);
		},

		disableGraphsBoundTo: function (bindto) {
			var graphsToDisable = $.grep(graphs, function (graph) {
				return graph.bindto == bindto
			});
			$.each(graphsToDisable, function (i, graphToDisable) {
				graphToDisable.disabled = true;
				graphToDisable.plot = null;
			});
		},

		activateGraph: function (graph) {
			if (graph) {
				graph.disabled = false;
				repaintGraph(graph);
			}
		},

		addGraph: function (pluginId, graph, metrics) {
			if (graph) {
				initGraph(pluginId, graph, metrics);
			}
		},

		onMetricsReceived: function (metrics) {
			updateGraphs(metrics);
		},

		onPluginSelected: function (pluginId) {
			selectedPluginId = pluginId;
			var graphsOfPlugin = $.grep(graphs, function (graph) {
				return graph.pluginId == pluginId;
			});

			$.each(graphsOfPlugin, function (i, graph) {
				repaintGraph(graph);
			});
		}
	};


	function initGraphs(plugins, metrics, onGraphsRendered) {
		$.each(plugins, function (i, plugin) {
			if (plugin.graphs) {
				$.each(plugin.graphs, function (i, graph) {
					initGraph(plugin.id, graph, metrics);
				});
			}
		});
		onGraphsRendered();
	}

	function initGraph(pluginId, graph, metrics) {
		graph.pluginId = pluginId;
		graphs.push(graph);
		var metricsForGraph = getAllMetricsForGraphWithValues(graph, metrics);
		var $bindTo = $(graph.bindto);
		if ($bindTo.length > 0) {
			$bindTo.css({height: "300px"});
			graph.series = [];
			graph.metadata = graph.metadata || {};
			$.each(metricsForGraph, function (graphName, value) {
				graph.metadata[graphName] = graph.metadata[graphName] || {};
				if (graph.derivative) {
					graph.metadata[graphName]["previousValue"] = value;
					value = 0;
				}

				var data = [
					[metrics.timestamp, value]
				];
				graph.metadata[graphName].data = data;
				graph.series.push({ label: graphName, data: data });
			});
			plotGraph(graph);
		}
	}

	function plotGraph(graph) {
		if (!graph.disabled && graph.pluginId == selectedPluginId) {
			graph.plot = $.plot($(graph.bindto), graph.series, {
				series: {
					stack: graph.stack,
					lines: {
						show: true,
						zero: false,
						fill: graph.fill,
						lineWidth: 2
					},
					shadowSize: 1
				},
				grid: {
					minBorderMargin: null,
					borderWidth: 0,
					hoverable: true,
					color: '#c8c8c8'
				},
				yaxis: {
					min: graph.min,
					max: graph.max,
					tickFormatter: formatters()[graph.format] || formatters()._default(graph.format)
				},
				xaxis: {
					mode: "time",
					minTickSize: [5, "second"]
				},
				tooltip: true,
				tooltipOpts: {
					content: "%s: %y"
				}
			});
		}
	}

	function updateGraphs(metrics) {
		$.each(graphs, function (i, graph) {
			extractNewValues(graph);
			repaintGraph(graph);
		});

		function extractNewValues(graph) {
			$.each(getAllMetricsForGraphWithValues(graph, metrics), function (graphName, value) {
				var value2 = value;
				var datapoints = graph.metadata[graphName].data;
				while (datapoints[0][0] < (metrics.timestamp - 60 * storedMinutes * 1000)) {
					datapoints.splice(0, 1);
				}
				if (graph.derivative === true) {
					var currentValue = value2;
					value2 = currentValue - graph.metadata[graphName].previousValue;
					graph.metadata[graphName].previousValue = currentValue;
				}
				datapoints.push([metrics.timestamp, value2]);
			});
		}
	}

	function repaintGraph(graph) {
		if (!graph.disabled && graph.pluginId == selectedPluginId) {
			if (!graph.plot) {
				plotGraph(graph);
			}
			$.each(graph.metadata, function (graphName, value) {
				var currentSeries = $.grep(graph.plot.getData(), function (s) {
					return s.label == graphName
				})[0];
				currentSeries.data = graph.metadata[graphName].data;
			});
			graph.plot.setData(graph.plot.getData());
			graph.plot.setupGrid();
			graph.plot.draw();
		}
	}

	function getAllMetricsForGraphWithValues(graph, data) {
		var metrics = {};
		$.each(graph.columns, function (i, metricPath) {
			var metricCategory = data[metricPath.metricCategory];
			// merge objects
			$.extend(metrics, findPropertyValuesByRegex(metricCategory, metricPath));
		});
		return metrics;
	}

	function findPropertyValuesByRegex(obj, metricPath) {
		var metrics = {};
		var matches = 1;
		$.each(obj, function(key, metric) {
			if (!(metricPath.metricPathRegex instanceof RegExp)) {
				metricPath.metricPathRegex = new RegExp(metricPath.metricPathRegex);
			}
			var match = metricPath.metricPathRegex.exec(key);
			if (match != null) {
				var graphName = metricPath.title || match[1] || match[0];
				var value = +metric[metricPath.metric] || 0; // convert "NaN" to 0
				metrics[graphName] = metrics[graphName] || 0;
				if (metricPath.aggregate === 'mean' && metrics) {
					metrics[graphName] = (metrics[graphName] * matches + value) / matches++;
				} else {
					// sum by default
					// deprecated behaviour, use metricPath.aggregate to explicitly set aggregation type
					metrics[graphName] = metrics[graphName] + value;
				}
			}
		});
		return metrics;
	}

	function formatters() {
		return {
			bytes: function (bytes) {
				if (bytes == 0) return '0 Byte';
				var k = 1024;
				var sizes = ['Bytes', 'KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];
				var i = Math.floor(Math.log(bytes) / Math.log(k));
				if (i < 0) {
					i = 0;
				}
				return round(bytes / Math.pow(k, i)) + ' ' + sizes[i];
			},
			percent: function (percent) {
				return round(percent) + ' %';
			},
			ms: function (size) {
				if (size === null) {
					return "";
				}

				if (Math.abs(size) < 1000) {
					return round(size) + " ms";
				}
				// Less than 1 min
				else if (Math.abs(size) < 60000) {
					return round(size / 1000) + " s";
				}
				// Less than 1 hour, devide in minutes
				else if (Math.abs(size) < 3600000) {
					return round(size / 60000) + " min";
				}
				// Less than one day, devide in hours
				else if (Math.abs(size) < 86400000) {
					return round(size / 3600000) + " hour";
				}
				// Less than one year, devide in days
				else if (Math.abs(size) < 31536000000) {
					return round(size / 86400000) + " day";
				}

				return round(size / 31536000000) + " year";
			},
			_default: function(unit) {
				return function(datapoint) {
					if (unit) {
						return round(datapoint) + " " + unit;
					} else {
						return round(datapoint);
					}
				};
			}
		}
	}

	function round(num, fractionDigits) {
		var e = fractionDigits || 2;
		var result = +(Math.round(num + ("e+" + e)) + ("e-" + e));
		if (isNaN(result)) {
			result = 0;
		}
		return result;
	}

}());
