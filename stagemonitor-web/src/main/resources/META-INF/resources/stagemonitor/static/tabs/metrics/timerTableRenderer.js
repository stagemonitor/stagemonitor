var timerTableRenderer = (function () {
	var timerTables = [];
	return {
		init: function (timerTable) {
			timerTables.push(timerTable);
			timerTable.table = $(timerTable.bindto).dataTable({
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
			timerTable.table.find('tbody').on('click', 'tr', function () {
				if (!$(this).hasClass('selected')) {
					// select
					timerTable.table.$('tr.selected').removeClass('selected');
					$(this).addClass('selected');
					timerTable.selectedName = timerTable.table.fnGetData(timerTable.table.fnGetPosition(this)).name;
					timerTable.onRowSelected();
				} else {
					// deselect
					timerTable.selectedName = null;
					$(this).removeClass('selected');
					timerTable.onRowDeselected();
				}
			});
		},
		onMetricsReceived: function (metrics) {
			$.each(timerTables, function (i, timerTable) {
				timerTable.table.fnClearTable();
				timerTable.table.fnAddData(getData(metrics, timerTable));
				restoreSelectedRow(timerTable.table);
			});
		}
	};

	function getData(metrics, timerTable) {
		var data = [];
		for (timerName in metrics.timers) {
			var match = timerTable.timerRegex.exec(timerName);
			if (match != null) {
				var timer = utils.clone(metrics.timers[timerName]);
				timer.name = match[1];

				// round all numbers
				$.each(timer, function (timerMetric, timerMetricValue) {
					if (typeof timerMetricValue == 'number') {
						timer[timerMetric] = timerMetricValue.toFixed(2)
					}
				});
				data.push(timer);
				timerTable.timers = timerTable.timers || {};
				if (!timerTable.timers[timer.name]) {
					timerTable.onNewTimer(timer.name, metrics);
					timerTable.timers[timer.name] = true;
				}
			}
		}
		return data;
	}

	function restoreSelectedRow(table) {
		$(table.fnSettings().aoData).each(function () {
			var requestName = table.fnGetData(this.nTr).name;
			if (requestName == selectedName) {
				$(this.nTr).addClass('selected');
			}
		});
	}
})();