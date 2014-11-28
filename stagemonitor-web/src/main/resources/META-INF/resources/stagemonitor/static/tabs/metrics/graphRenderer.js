var graphRenderer = (function () {
	var storedMinutes = 5;
	var tickMs = 1000;
	var metrics = [];
	var graphs = [];

	function isDisabled(graph, graphName) {
		return (graph.disabledLines || []).indexOf(graphName) != -1;
	}

	function initGraphs(newGraphs, onGraphsRendered) {
		graphs = graphs.concat(newGraphs);
		getMetricsFromServer(function (metrics) {
			$.each(graphs, function (i, graph) {
				var metricsForGraph = getAllMetricsForGraphWithValues(graph, metrics);
				var $bindTo = $(graph.bindto);
				if ($bindTo.length > 0) {
					$bindTo.css({height: "300px"});
					var series = [];
					$.each(metricsForGraph, function (graphName, value) {
						if (graph.derivative) {
							graph[graphName] = { previousValue: value};
							value = 0;
						}

//						graph[graphName].data = data;
						series.push({label: graphName, data: [ [metrics.timestamp, value] ] });
					});
					$.plot($bindTo, series, {
						series: {
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
							tickFormatter: formatters[graph.format]
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
			});
			onGraphsRendered();
		});
	}

	function getMetricsFromServer(callback) {
//		$.getJSON(contextPath + "/stagemonitor/metrics", function(data) {
		$.getJSON("http://localhost:8880/petclinic/stagemonitor/metrics", function (metrics) {
			var date = new Date();
			metrics['timestamp'] = date.getTime();
			callback(metrics);
		});
	}

	function updateGraphs(metrics) {
		$.each(graphs, function (i, graph) {
			var plot = $(graph.bindto).data("plot");
			if (plot) {
				var series = plot.getData();

				$.each(getAllMetricsForGraphWithValues(graph, metrics), function (graphName, value) {
					updateDatapoints(graphName, value);
				});

				function updateDatapoints(graphName, value) {
					var currentSeries = $.grep(series, function (s) {
						return s.label == graphName
					})[0];
					var datapoints = currentSeries.data;

					while (datapoints[0][0] < (metrics.timestamp - 60 * storedMinutes * 1000)) {
						datapoints.splice(0, 1);
					}
					if (graph.derivative === true) {
						var currentValue = value;
						value = currentValue - graph[graphName].previousValue;
						graph[graphName].previousValue = currentValue;
					}
					datapoints.push([metrics.timestamp, value]);
				}
				plot.setData(series);
				plot.setupGrid();
				plot.draw();
			}
		});
	}

	function getAllMetricsForGraphWithValues(graph, data) {
		var metrics = {};
		$.each(graph.columns, function (i, metricPath) {
			var metricCategory = data[metricPath[0]];
			var regex = metricPath[1];
			var valuePath = metricPath[2];
			// merge objects
			$.extend(metrics, findPropertyValuesByRegex(metricCategory, regex, valuePath));
		});
		return metrics;
	}

	var findPropertyValuesByRegex = function (obj, regex, valuePath) {
		var metrics = {};
		var key;
		for (key in obj) {
			var match = new RegExp(regex).exec(key);
			if (match != null) {
				metrics[match[1] || match[0]] = obj[key][valuePath];
			}
		}
		return metrics;
	};

	var formatters = {
		bytes: function (bytes) {
			if (bytes == 0) return '0 Byte';
			var k = 1024;
			var sizes = ['Bytes', 'KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];
			var i = Math.floor(Math.log(bytes) / Math.log(k));
			return (bytes / Math.pow(k, i)).toPrecision(3) + ' ' + sizes[i];
		},
		percent: function (percent) {
			return round(percent * 100) + ' %';
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
		}
	};

	function round(num, fractionDigits) {
		var e = fractionDigits || 2;
		return +(Math.round(num + ("e+" + e)) + ("e-" + e));
	}

	return {
		renderGraphs: function (graphs, onMetricsReceived, onGraphsRendered) {
			initGraphs(graphs, onGraphsRendered);
			setInterval(function () {
				getMetricsFromServer(function (metrics) {
					updateGraphs(metrics);
					onMetricsReceived(metrics);
				});
			}, tickMs);
		}
	}
}());
