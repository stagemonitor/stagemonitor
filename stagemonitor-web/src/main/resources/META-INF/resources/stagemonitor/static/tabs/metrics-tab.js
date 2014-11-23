var metricsListeners = [];
var dataModel = {
	data: [],
	addData: function (d) {
		var maxElements = 50;
		if (dataModel.data.length >= maxElements) {
			dataModel.data.splice(0, 1);
		}
		dataModel.data.push(d);
	},
	getColumn: function (name, dataExtractor) {
		var column = [name];
		for (var i = 0; i < dataModel.data.length; i++) {
			var d = dataModel.data[i];
			column.push(dataExtractor(d));
		}
		return column;
	}
};

function renderMetricsTab(contextPath) {
	var chart = c3.generate({
		bindto: '#chart',
		data: {
			x: 'x',
			xFormat: '%H:%M:%S',
			columns: [
			]
		},
		axis: {
			x: {
				type: 'timeseries',
				tick: {
					format: '%H:%M:%S'
				}
			},
			y: {
				min: 0,
				padding: {bottom: 0},
				tick: {
					format: d3.format("s")
				}
			}
		}
	});
	metricsListeners.push(function () {
		chart.load({
			columns: [
				dataModel.getColumn('x', function (data) {
					return data.time
				}),
				dataModel.getColumn('max', function (data) {
					return data.gauges["jvm.memory.heap.max"].value
				}),
				dataModel.getColumn('commited', function (data) {
					return data.gauges["jvm.memory.heap.committed"].value
				}),
				dataModel.getColumn('used', function (data) {
					return data.gauges["jvm.memory.heap.used"].value
				})
			]
		});
	});


	function getMetrics() {
//		$.getJSON(contextPath + "/stagemonitor/metrics", function(data) {
		$.getJSON("http://localhost:8880/petclinic/stagemonitor/metrics", function (data) {
			var date = new Date();
			data['time'] = (date.getHours() -12)+ ':' + date.getMinutes() + ':' + date.getSeconds();
			dataModel.addData(data);
			for (var i = 0; i < metricsListeners.length; i++) {
				metricsListeners[i]();
			}
		});
	}
	setInterval(getMetrics, 1000);

}