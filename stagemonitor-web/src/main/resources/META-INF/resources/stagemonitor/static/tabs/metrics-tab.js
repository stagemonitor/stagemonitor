function renderMetricsTab(contextPath) {
	var storedMinutes = 5;
	var tickMs = 1000;
	var plugins = [
		{
			label: "JVM",
			htmlPath: "tabs/metrics/jvm-metrics.html",
			jsPath: "tabs/metrics/jvm-metrics.js",
			pluginVariableName: "jvmPlugin",
			id: "jvm-metrics"
		},
		{
			label: "JVM2",
			htmlPath: "tabs/metrics/jvm-metrics.html",
			jsPath: "tabs/metrics/jvm-metrics.js",
			pluginVariableName: "jvmPlugin",
			id: "jvm-metrics2"
		}
	];

	var $metricPlugins = $("#metric-plugins");
	var $sideMenu = $("#side-menu");
	var graphs = [];
	$.each(plugins, function (i, plugin) {
		$sideMenu.append('<li class="plugin-link' + (i == 0 ? ' active' : '') + '">' +
			'	<a href="#' + plugin.id + '">' + plugin.label + '</a>' +
			'</li>');
		$metricPlugins.append('<div id="' + plugin.id + '" class="metric-plugin' + (i != 0 ? ' hidden' : '') + '"></div>');
		// TODO contextPath
		$('#'+plugin.id).load(plugin.htmlPath, function() {
			$.getScript(plugin.jsPath, function() {
				plugin["pluginVariable"] = window[plugin.pluginVariableName];
				graphs = graphs.concat(plugin.pluginVariable.getGraphs());
			});
		})
	});

	// select a plugin from the side bar
	$sideMenu.find("a").click(function () {
		var thisLink = $(this);
		$(".plugin-link").removeClass("active");
		thisLink.parent().addClass("active");
		$(".metric-plugin").addClass("hidden");
		$(thisLink.attr("href")).removeClass("hidden");
		return false;
	});

	function getMetricsFromServer(callback) {
//		$.getJSON(contextPath + "/stagemonitor/metrics", function(data) {
		$.getJSON("http://localhost:8880/petclinic/stagemonitor/metrics", function (data) {
			var date = new Date();
			data['timestamp'] = date.getTime();
			callback(data);

			// notify plugins about new metrics
			$.each(plugins, function (i, plugin) {
				plugin.pluginVariable.onMetricsReceived(data);
			});
		});
	}

	$("#metrics-tab").find("a").one('click', function () {
		getMetricsFromServer(function (data) {
			$.each(graphs, function (i, graph) {
				var metrics = getAllMetricsWithValues(graph, data);
				var $bindTo = $(graph.bindto);
				if ($bindTo.length > 0) {
					$bindTo.css({height: "300px"});
					var series = [];
					$.each(metrics, function (graphName, value) {
						if (graph.derivative) {
							graph[graphName] = { previousValue: value};
							value = 0;
						}
						series.push({label: graphName, data: [
							[data.timestamp, value]
						] });
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
		});

		setInterval(function () {
			getMetricsFromServer(updateGraphs)
		}, tickMs);
	});

	// update graphs
	function updateGraphs(data) {
		$.each(graphs, function (i, graph) {
			var plot = $(graph.bindto).data("plot");
			if (plot) {
				var series = plot.getData();
				var j = 0;
				function updateDatapoints(key, value) {
					var datapoints = series[j++].data;
					while (datapoints[0][0] < (data.timestamp - 60 * storedMinutes * 1000)) {
						datapoints.splice(0, 1);
					}
					if (graph.derivative === true) {
						var currentValue = value
						value = currentValue - graph[key].previousValue;
						graph[key].previousValue = currentValue;
					}
					datapoints.push([data.timestamp, value]);
				}

				$.each(getAllMetricsWithValues(graph, data), function (key, value) {
					updateDatapoints(key, value);
				});

				plot.setData(series);
				plot.setupGrid();
				plot.draw();
			}
		});
	}

	function getAllMetricsWithValues(graph, data) {
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

	var findPropertyValuesByRegex = function(obj, regex, valuePath) {
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
			return (percent * 100).toFixed(2) + '%';
		},
		ms: function(size) {
			if (size === null) { return ""; }

			if (Math.abs(size) < 1000) {
				return size.toFixed(1) + " ms";
			}
			// Less than 1 min
			else if (Math.abs(size) < 60000) {
				return (size / 1000).toFixed(1)  + " s";
			}
			// Less than 1 hour, devide in minutes
			else if (Math.abs(size) < 3600000) {
				return (size / 60000).toFixed(1) + " min";
			}
			// Less than one day, devide in hours
			else if (Math.abs(size) < 86400000) {
				return (size / 3600000).toFixed(1) + " hour";
			}
			// Less than one year, devide in days
			else if (Math.abs(size) < 31536000000) {
				return (size / 86400000).toFixed(1) + " day";
			}

			return (size / 31536000000).toFixed(1) + " year";
		}
	};
}

