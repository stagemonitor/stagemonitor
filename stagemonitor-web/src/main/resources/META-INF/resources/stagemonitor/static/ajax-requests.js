var rootRequestTrace;
var noOfRequestTraces = 0;
listenForAjaxRequestTraces = function (rootRequest, websocketConnectionId) {
	rootRequestTrace = rootRequest;
	renderAjaxRequestTraces(rootRequest);
	try {
		var webSocket = new WebSocket("ws://" + window.location.host + "/stagemonitor/request-trace/" + websocketConnectionId);
		webSocket.onmessage = function (event) {
			noOfRequestTraces++;
			renderAjaxRequestTraces(JSON.parse(event.data));
		};

	} catch (e) {
		// no websocket support
		console.log(e);
	}
};


$(document).ready(function () {
	$("#ajax-table").dataTable();

});

renderAjaxRequestTraces = function (data) {
	if (noOfRequestTraces > 0) {
		$("#ajax-tab-link").removeClass("hidden");
		$("#ajax-badge").html(noOfRequestTraces);
	}

	$("#ajax-table").DataTable().row.add([
		data["@timestamp"],
		data.name,
		data.url,
		data.executionTime,
		data.statusCode,
		data.method,
		data
	]).draw();

	$('#ajax-table').find('tbody').on('click', 'tr', function () {
		if (!$(this).hasClass('selected')) {
			// select
			var table = $("#ajax-table").dataTable();
			table.$('tr.selected').removeClass('selected');
			$(this).addClass('selected');
			var data = table.dataTable().fnGetData(table.fnGetPosition(this))[6];
			renderRequestTab(data);
			renderCallTree(data);
		}
	});
};