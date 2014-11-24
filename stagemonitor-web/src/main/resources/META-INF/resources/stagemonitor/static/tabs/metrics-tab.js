function renderMetricsTab(contextPath) {
	var storedMinutes = 5;
	var tickMs = 1000;

	function getMetricsFromServer(callback) {
//		$.getJSON(contextPath + "/stagemonitor/metrics", function(data) {
		$.getJSON("http://localhost:8880/petclinic/stagemonitor/metrics", function (data) {
			var date = new Date();
			data['timestamp'] = date.getTime();
			callback(data);
		});
	}

	var graphs = [
		{
			bindto: '#memory',
			min: 0,
			format: bytes,
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
			format: percent,
			columns: [
				["gauges", /jvm.memory.pools.([^\.]+).usage/, "value"]
			]
		},
		{
			bindto: '#cpu',
			min: 0,
			max: 1,
			fill: 0.1,
			format: percent,
			columns: [
				["gauges", "jvm.cpu.process.(usage)", "value"]
			],
			padding: {bottom: 0, top: 0 }
		}
	];

	function bytes(bytes) {
		if(bytes == 0) return '0 Byte';
		var k = 1024;
		var sizes = ['Bytes', 'KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];
		var i = Math.floor(Math.log(bytes) / Math.log(k));
		return (bytes / Math.pow(k, i)).toPrecision(3) + ' ' + sizes[i];
	}

	function percent(percent) {
		return (percent * 100).toFixed(2) + '%';
	}

	var first = true;
	$(window).resize(function () {
		if (first) {
			first = false;
			getMetricsFromServer(function (data) {
				$.each(graphs, function (i, graph) {
					var metrics = getAllMetricsWithValues(graph, data);
					var $bindTo = $(graph.bindto);
					$bindTo.css({height: "300px"});
					var series = [];
					$.each(metrics, function (graphName, value) {
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
							,labelMargin: 20
						},
						yaxis: {
							min: graph.min,
							max: graph.max,
							tickFormatter: graph.format
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


				});
			});

			setInterval(function () {
				getMetricsFromServer(updateGraphs)
			}, tickMs);
		}
	});

	// update graphs
	function updateGraphs(data) {
		$.each(graphs, function (i, graph) {
			var plot = $(graph.bindto).data("plot");
			var series = plot.getData();

			var j = 0;
			function updateDatapoints(key, value) {
				var datapoints = series[j++].data;
				while (datapoints[0][0] < (data.timestamp - 60 * storedMinutes * 1000)) {
					datapoints.splice(0, 1);
				}
				datapoints.push([data.timestamp, value]);
			}

			$.each(getAllMetricsWithValues(graph, data), function (key, value) {
				updateDatapoints(key, value);
			});

			plot.setData(series);
			plot.setupGrid();
			plot.draw();
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

}