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
		initializeDatatables();
		$html.filter("#callStackLightbox").insertAfter("#hitogramLightbox");
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

function initializeDatatables() {
	requestDataTable = $('#request-table').dataTable({
		"bJQueryUI": true,
		"sPaginationType": "full_numbers",
		"bLengthChange": false
	});
	$("#request-table").on('click', "tbody tr", function (e) {
		var requestName = e.currentTarget.childNodes[0].innerHTML;
		setRequestName(requestName);
		originalUpdateGraphs();
		if (selectedRequestName == '*') {
			$('#stacktraces-table').dataTable().fnClearTable();
		} else {
			$('#stacktraces-table').dataTable().fnReloadAjax("/executionContexts/search" + getAppEnvHostQueryParams() + "&name=" + requestName);
		}

		highlightSelectedRow(this, requestDataTable);
	});
	var stackDataTable = $('#stacktraces-table').dataTable({
		"bJQueryUI": true,
		"sPaginationType": "full_numbers",
		"bLengthChange": false
	});
	$("#stacktraces-table").on('click', "tbody tr", function (e) {
		var id = e.currentTarget.childNodes[0].innerHTML;
		highlightSelectedRow(this, stackDataTable);
		loadCallStack(id)
	});
	reloadRequestTable();
}

function loadCallStack(id) {
	var callStack = $("#call-stack");
	callStack.text("");
	callStack.css("height", $(window).height() - 250);
	$(".lightbox-content").css("width", $(window).width() - 100);
	$(".lightbox-content").css("height", $(window).height() - 100);
	$('#callStackLightbox').lightbox({
		resizeToFit: false
	});
	$.ajax({
		url: "/executionContexts/" + id,
		dataType: 'text',
		success: function (data) {
			callStack.text(data);
		}
	});
}

function highlightSelectedRow(thiz, datatable) {
	if ($(thiz).hasClass('row_selected')) {
		$(thiz).removeClass('row_selected');
	}
	else {
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
					for(var i = 0; i< matches.length; i++) {
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
