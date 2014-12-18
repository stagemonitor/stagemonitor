(function () {
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

	var requestMetricsPlugin = {
		id: "request-metrics",
		label: "Requests",
		graphs: [ responseTimeGraphs.All, throughputGraphs.All ],
		timerTable: {
			bindto: "#request-table",
			timerRegex: /request.([^\.]+).server.time.total/,
			onNewTimer: onNewTimer,
			onRowSelected: onRowSelected,
			onRowDeselected: onRowDeselected
		}
	};
	plugins.push(
		requestMetricsPlugin
	);

	function onRowSelected() {
		graphRenderer.disableGraphsBoundTo("#time");
		graphRenderer.disableGraphsBoundTo("#throughput");
		graphRenderer.activateGraph(responseTimeGraphs[requestMetricsPlugin.timerTable.selectedName]);
		graphRenderer.activateGraph(throughputGraphs[requestMetricsPlugin.timerTable.selectedName]);
	}

	function onRowDeselected() {
		graphRenderer.disableGraphsBoundTo("#time");
		graphRenderer.disableGraphsBoundTo("#throughput");
		graphRenderer.activateGraph(responseTimeGraphs['All']);
		graphRenderer.activateGraph(throughputGraphs['All']);
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

}());

