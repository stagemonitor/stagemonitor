var rootRequestTrace;
var noOfRequestTraces = 0;
listenForAjaxRequestTraces = function (rootRequest, websocketConnectionId) {
	rootRequestTrace = rootRequest;
	try {
		var webSocket = new WebSocket("ws://" + window.location.host + "/stagemonitor/request-trace/" + websocketConnectionId);
		webSocket.onmessage = function (event) {
			var data = JSON.parse(event.data);
			addAjaxRequestTrace(data);
		};

	} catch (e) {
		// no websocket support
		console.log(e);
	}
};


$(document).ready(function () {
	var table = $("#ajax-table").dataTable({
		dom: '<"toolbar">lfrtip',
		"columns": [
			{ "data": "@timestamp" },
			{ "data": "name" },
			{ "data": "url" },
			{ "data": "executionTime" },
			{ "data": "statusCode" },
			{ "data": "method" }
		]
	});
	table.find('tbody').on('click', 'tr', function () {
		if (!$(this).hasClass('selected')) {
			// select
			table.$('tr.selected').removeClass('selected');
			$(this).addClass('selected');
			var data = table.fnGetData(table.fnGetPosition(this));
			renderRequestTab(data);
			renderCallTree(data);
		} else {
			// deselect
			$(this).removeClass('selected');
			renderRequestTab(rootRequestTrace);
			renderCallTree(rootRequestTrace);
		}
	});
	function addToolbar() {
		var toolbar = $("div.toolbar");
		toolbar.html('<span id="clear-ajax" class="tip glyphicon glyphicon-ban-circle" data-toggle="tooltip" ' +
			'data-placement="right" title="Clear"></span> ');
		$("#clear-ajax").click(function () {
			renderRequestTab(rootRequestTrace);
			renderCallTree(rootRequestTrace);
			table.fnClearTable();
			noOfRequestTraces = 0;
			$("#ajax-badge").html("");
		});
		toolbar.append('<span id="autoscoll-ajax" class="tip glyphicon glyphicon-chevron-down" data-toggle="tooltip" ' +
			'data-placement="right" title="Auto select latest ajax request"></span> ');
		var $autoscoll = $("#autoscoll-ajax");
		if ("true" == localStorage.getItem("stagemonitor-widget-ajax-autoscroll")) {
			$autoscoll.addClass('active');
		}
		$autoscoll.click(function () {
			$(this).toggleClass("active");
			localStorage.setItem("stagemonitor-widget-ajax-autoscroll", $(this).hasClass("active"));
		});
		toolbar.append('<span id="autoscoll-ajax" class="tip glyphicon glyphicon glyphicon-question-sign" ' +
			'data-toggle="tooltip" data-placement="right" title="Select row to analyze a particular ajax request. Deselect to analyze the root request."></span>');
	}
	addToolbar();
	// TODO testcode
//	addAjaxRequestTrace(rootRequestTrace);
//	addAjaxRequestTrace(rootRequestTrace);
});

addAjaxRequestTrace = function (data) {
	noOfRequestTraces++;
	if (noOfRequestTraces > 0) {
		$("#ajax-badge").html(noOfRequestTraces);
		$("#ajax-tab-link").removeClass("hidden");
	}
	var node = $("#ajax-table").DataTable().row.add(data).draw().node();
	if ("true" == localStorage.getItem("stagemonitor-widget-ajax-autoscroll")) {
		renderRequestTab(data);
		renderCallTree(data);
		$("#ajax-table").find("tr.selected").removeClass('selected');
		$(node).addClass('selected');
	}
};