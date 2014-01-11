var requestDataTable;
var originalUpdateGraphs;
var selectedRequestName;
$(document).ready(function () {
	// hook into dashboard.js#renderView to initializeRequestTable
	renderView = interceptFunction(renderView, initializeRequestTable);

	//  intercept dashboard.js#applyParameters to add requestName parameters
	applyParameters = interceptFunction(applyParameters, function () {
		config.parameters.requestName = { requestName: {requestName: selectedRequestName}};
	}, function () {
		// Afterwards, delete requestName from parameters so that it won't be rendered as a dropdown
		delete config.parameters.requestName;
	})
});

function initializeRequestTable() {
	if (config.type == 'request') {
		setRequestName('*');
		console.log(config.parameters);
		interceptUpdateGraphs();
		loadAdditionalHtml();
	}
}

function loadAdditionalHtml() {
	$.get("../requestTable.html", function (html) {
		var $html = $(html);
		$html.filter("#requstTable").insertBefore("#dashboards-view");
		$html.filter("#callStackLightbox").insertAfter("#hitogramLightbox");
		initializeDatatables();
	});
}

function interceptUpdateGraphs() {
	originalUpdateGraphs = updateGraphs;
	updateGraphs = interceptFunction(updateGraphs, reloadRequestTable);
}

function setRequestName(requestName) {
	requestName = encodeForGraphite(requestName);
	if (requestName == selectedRequestName) {
		selectedRequestName = '*'
	} else {
		selectedRequestName = requestName;
	}
}

function findStackTraces(requestName) {
	$("#call-stack").text("");
	$('#stacktraces-table').dataTable().fnClearTable();
	$('#stacktraces-table').dataTable().fnReloadAjax("/executionContexts/search" + getAppEnvHostQueryParams() + "&name=" + requestName);
	$(".lightbox-content").css("width", $(window).width() - 100);
	$(".lightbox-content").css("height", $(window).height() - 100);
	$('#callStackLightbox').lightbox({
		resizeToFit: false
	});
}
function initializeDatatables() {
	requestDataTable = $('#request-table').dataTable({
		"bJQueryUI": true,
		"sPaginationType": "full_numbers",
		"bLengthChange": false,
		"aoColumns": [
			{
				"sTitle": "Request Name",
				"mData": "name"
			},
			{
				"sTitle": "Requests/ min",
				"mData": "m1_rate"
			},
			{
				"sTitle": "Errors",
				"mData": "error"
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
			},
			{   "mData": null,
				"fnRender": function (oObj) {
					return '<button class="btn btn-small btn-primary" onclick="findStackTraces(\'' + oObj.aData.name + '\');">Call&nbsp;Stack</button>';
				}
			}
		]
	});
	$("#request-table").on('click', "tbody tr :not(button)", function (e) {
		// TODO not when clicked on button
		var requestName = requestDataTable.fnGetData(this).name;
		setRequestName(requestName);
		originalUpdateGraphs();
		highlightSelectedRow(this, requestDataTable);
	});
	var stackDataTable = $('#stacktraces-table').dataTable({
		"bJQueryUI": true,
		"sPaginationType": "full_numbers",
		"bLengthChange": false,
		"bFilter": false,
		"aoColumns": [
			{
				"sTitle": "Date",
				"mData": "date"
			},
			{
				"sTitle": "Time (ms)",
				"mData": "time"
			},
			{
				"sTitle": "URL",
				"mData": "url"
			},
			{
				"sTitle": "Status",
				"mData": "status"
			}
		]
	});
	$("#stacktraces-table").on('click', "tbody tr", function (e) {
		var id = stackDataTable.fnGetData(this).id;
		var onSelect = function () {
			loadCallStack(id)
		};
		var onDeselect = function () {
			$("#call-stack").text("");
		};
		highlightSelectedRow(this, stackDataTable, onSelect, onDeselect);
	});
	reloadRequestTable();
}

function loadCallStack(id) {
	var callStack = $("#call-stack");
	callStack.text("");
	$("#stacktraces-lightbox-inner").css("height", $(window).height() - 250);
	$("#stacktraces-lightbox-inner").css("max-height", $(window).height() - 250);
	$.ajax({
		url: "/executionContexts/" + id,
		dataType: 'text',
		success: function (data) {
			callStack.text(data);
		}
	});
}

function highlightSelectedRow(thiz, datatable, onSelect, onDeselect) {
	if ($(thiz).hasClass('row_selected')) {
		if (onDeselect != null) onDeselect();
		$(thiz).removeClass('row_selected');
	} else {
		if (onSelect != null) onSelect();
		datatable.$('tr.row_selected').removeClass('row_selected');
		$(thiz).addClass('row_selected');
	}
}

function reloadRequestTable() {
	if (config.type == 'request') {
		if (requestDataTable != undefined) {
			var selectedRow = requestDataTable.$('tr.row_selected');
			var selectedRequestName = selectedRow.length > 0 ? selectedRow[0].childNodes[0].innerHTML : null;
			$('#request-table').dataTable().fnReloadAjax(getRequestTableUrl(), function () {
				if (selectedRequestName != null) {
					var matches = $('#request-table tr:contains(' + selectedRequestName + ')');
					for (var i = 0; i < matches.length; i++) {
						match = matches[i];
						if (match.childNodes[0].innerHTML == selectedRequestName) {
							$(match).addClass('row_selected');
							break;
						}
					}
				}
			});
		}
	}
}

function getRequestTableUrl() {
	return "/graphitus/requestTable"
		+ getAppEnvHostQueryParams()
		+ getRangeQueryParams();
}

function getRangeQueryParams() {
	var range = "";
	var timeBack = $('#timeBack').val();
	var start = $('#start').val();
	var end = $('#end').val();
	if (timeBack != "") {
		range = "&from=-" + parseTimeBackValue(timeBack);
	} else if (start != "" && end != "") {
		var startParts = start.split(" ");
		var endParts = end.split(" ");
		range = "&from=" + startParts[1] + "_" + startParts[0] + "&until=" + endParts[1] + "_" + endParts[0];
	}
	return range;
}

function getAppEnvHostQueryParams() {
	return "?application=" + getDecodedParameter("application")
		+ "&environment=" + getDecodedParameter("environment")
		+ "&host=" + getDecodedParameter("host");
}

function getDecodedParameter(paramGroupName) {
	var selectedParamText = $('#' + paramGroupName + " option:selected").text();
	return config.parameters[paramGroupName][selectedParamText][paramGroupName]
}

/* UTILS */

function encodeForGraphite(requestName) {
	return requestName.replaceAll(".", ":").replaceAll(" ", "_").replaceAll("/", "|");
}

String.prototype.replaceAll = function (search, replacement) {
	return this.toString().split(search).join(replacement);
};

function interceptFunction(funktion, before, after) {
	return function () {
		if (before != null) {
			before();
		}
		try {
			return funktion.apply(this, arguments);
		} finally {
			if (after != null) {
				after();
			}
		}
	};
}
