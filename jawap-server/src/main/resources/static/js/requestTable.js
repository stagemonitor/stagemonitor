var requestDataTable;
var originalUpdateGraphs;
var selectedRequestName;
$(document).ready(function () {
	// hook into dashboard.js#renderView to initializeRequestTable
	renderView = interceptFunction(renderView, initializeRequestTable, function() {
		// I don't like the histogram ...
		$(".fa-bar-chart-o").parent().remove();
	});

	//  intercept dashboard.js#applyParameters to add requestName parameters
	applyParameters = interceptFunction(applyParameters, function () {
		config.parameters.requestName = { requestName: {requestName: selectedRequestName}};
	}, function () {
		// Afterwards, delete requestName from parameters so that it won't be rendered as a dropdown
		delete config.parameters.requestName;
	});

//	applyRegexToName = interceptFunction(applyRegexToName, function(args) {
//		args[1] = decodeForGraphite(args[1]);
//	});
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

function findCallStacks(requestName) {
	$("#call-stack").text("");
	$('#callstacks-table').dataTable().fnClearTable();
	$('#callstacks-table').dataTable().fnReloadAjax("/executionContexts/search" + getAppEnvHostQueryParams() + "&name=" + requestName);
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
					var name = oObj.aData.name;
					if (name != '- Total -')
						return '<button class="btn-callstack btn btn-small btn-primary" onclick="findCallStacks(\'' + name + '\');">Call&nbsp;Stack</button>';
					else return null;
				}
			}
		]
	});
	$("#request-table").on('click', "tbody tr", function (e) {
		var requestName = requestDataTable.fnGetData(this).name;
		if (requestName != '- Total -') {
			setRequestName(requestName);
			originalUpdateGraphs();
			highlightSelectedRow(this, requestDataTable);
		}
	});
	// no (de)selection should happen when clicking on button
	// -> trigger highlightSelectedRow 2nd time so that (de)selection is undone
	$("#request-table").on('click', ".btn-callstack", function (e) {
		highlightSelectedRow(this.parentNode.parentNode, requestDataTable);
	});
	var stackDataTable = $('#callstacks-table').dataTable({
		"bJQueryUI": true,
		"sPaginationType": "full_numbers",
		"bLengthChange": false,
		"bFilter": false,
		"bInfo": false,
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
	$("#callstacks-table-div .fg-toolbar").first().remove();
	$("#callstacks-table").on('click', "tbody tr", function (e) {
		var id = stackDataTable.fnGetData(this).id;
		var onSelect = function () {
			loadCallStack(id)
		};
		var onDeselect = function () {
			$("#call-stack").hide();
		};
		highlightSelectedRow(this, stackDataTable, onSelect, onDeselect);
	});
	reloadRequestTable();
}

function loadCallStack(id) {
	var callStack = $("#call-stack");
	$("#callstacks-lightbox-inner").css("height", $(window).height() - 250);
	$("#callstacks-lightbox-inner").css("max-height", $(window).height() - 250);
	$.ajax({
		url: "/executionContexts/" + id,
		dataType: 'text',
		success: function (data) {
			callStack.text(data);
			callStack.show()
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
			}, true);
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
		+ "&instance=" + getDecodedParameter("instance")
		+ "&host=" + getDecodedParameter("host");
}

function getDecodedParameter(paramGroupName) {
	var selectedParamText = $('#' + paramGroupName + " option:selected").text();
	return encodeURIComponent(config.parameters[paramGroupName][selectedParamText][paramGroupName])
}

/* UTILS */

function encodeForGraphite(requestName) {
	return encodeURIComponent(requestName).replaceAll(".", "%2e").replaceAll("+", "%20");
}
function decodeForGraphite(requestName) {
	return decodeURIComponent(requestName);
}

String.prototype.replaceAll = function (search, replacement) {
	return this.toString().split(search).join(replacement);
};

function interceptFunction(funktion, before, after) {
	return function () {
		if (before != null) {
			before(arguments);
		}
		try {
			return funktion.apply(this, arguments);
		} finally {
			if (after != null) {
				after(arguments);
			}
		}
	};
}
