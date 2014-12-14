(function () {
	var table;
	var selectedName;
	var responseTimeGraphs = {
		All: {
			bindto: '#time',
			min: 0,
			format: 'ms',
			fill: 0.1,
			columns: [
				["timers", /request.(All).server.time.total/, "mean"]
			]
		}
	};
	var throughputGraphs = {
		All: {
			bindto: '#throughput',
			min: 0,
			format: 'requests/sec',
			fill: 0.1,
			columns: [
				["timers", /request.(All).server.time.total/, "m1_rate"]
			]
		}
	};

	plugins.push(
		{
			id: "request-metrics",
			label: "Requests",
			htmlPath: "tabs/metrics/request-metrics.html",
			graphs: [ responseTimeGraphs.All, throughputGraphs.All ],
			timerTable: {
				bindto: "#request-table",
				timerRegex: /request.([^\.]+).server.time.total/
			},
			onHtmlInitialized: function () {
				initRequestTable();
			},
			onMetricsReceived: function (metrics) {
				table.fnClearTable();
				table.fnAddData(getData(metrics));
				restoreSelectedRow();
			}
		}
	);

	function restoreSelectedRow() {
		$(table.fnSettings().aoData).each(function () {
			var requestName = table.fnGetData(this.nTr).name;
			if (requestName == selectedName) {
				$(this.nTr).addClass('selected');
			}
		});
	}

	function onRowSelected() {
		graphRenderer.disableGraphsBoundTo("#time");
		graphRenderer.disableGraphsBoundTo("#throughput");
		graphRenderer.activateGraph(responseTimeGraphs[selectedName]);
		graphRenderer.activateGraph(throughputGraphs[selectedName]);
	}

	function onRowDeselected() {
		graphRenderer.disableGraphsBoundTo("#time");
		graphRenderer.disableGraphsBoundTo("#throughput");
		graphRenderer.activateGraph(responseTimeGraphs['All']);
		graphRenderer.activateGraph(throughputGraphs['All']);
	}

	function initRequestTable() {
		table = $('#request-table').dataTable({
			"bJQueryUI": true,
			"sPaginationType": "full_numbers",
			"bLengthChange": false,
			"aoColumns": [
				{
					"sTitle": "Name",
					"mData": "name"
				},
				{
					"sTitle": "Requests/s",
					"mData": "m1_rate"
				},
				{
					"sTitle": "Max",
					"mData": "max"
				},
				{
					"sTitle": "Mean",
					"mData": "mean"
				},
				{
					"sTitle": "Min",
					"mData": "min"
				},
				{
					"sTitle": "p50",
					"mData": "p50"
				},
				{
					"sTitle": "p95",
					"mData": "p95"
				},
				{
					"sTitle": "Std. Dev.",
					"mData": "stddev"
				}
			]
		});
		table.find('tbody').on('click', 'tr', function () {
			if (!$(this).hasClass('selected')) {
				// select
				table.$('tr.selected').removeClass('selected');
				$(this).addClass('selected');
				selectedName = table.fnGetData(table.fnGetPosition(this)).name;
				onRowSelected();
			} else {
				// deselect
				selectedName = null;
				$(this).removeClass('selected');
				onRowDeselected();
			}
		});
	}

	function getData(metrics) {
		var data = [];
		for (timerName in metrics.timers) {
			var match = new RegExp(/request.([^\.]+).server.time.total/).exec(timerName);
			if (match != null) {
				var timer = metrics.timers[timerName];
				timer.name = match[1];

				// round all numbers
				$.each(timer, function (timerMetric, timerMetricValue) {
					if (typeof timerMetricValue == 'number') {
						timer[timerMetric] = timerMetricValue.toFixed(2)
					}
				});
				data.push(timer);
				if (!responseTimeGraphs[timer.name]) {
					onNewTimer(timer.name, metrics);
				}
			}
		}
		return data;
	}

	function onNewTimer(graphName, metrics) {
		responseTimeGraphs[graphName] = {
			bindto: '#time',
			disabled: true,
			min: 0,
			format: 'ms',
			fill: 0.1,
			columns: [
				["timers", "request.(" + RegExp.quote(graphName) + ").server.time.total", "mean"]
			]
		};
		graphRenderer.addGraph(responseTimeGraphs[graphName], metrics);

		throughputGraphs[graphName] = {
			bindto: '#throughput',
			disabled: true,
			min: 0,
			format: 'requests/sec',
			fill: 0.1,
			columns: [
				["timers", "request.(" + RegExp.quote(graphName) + ").server.time.total", "m1_rate"]
			]
		};
		graphRenderer.addGraph(throughputGraphs[graphName], metrics);
	}

	RegExp.quote = function(str) {
		return (str+'').replace(/([.?*+^$[\]\\(){}|-])/g, "\\$1");
	};
}());

