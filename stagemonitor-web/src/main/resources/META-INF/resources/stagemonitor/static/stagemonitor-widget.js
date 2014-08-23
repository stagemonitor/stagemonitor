$(document).ready(function() {
	var thresholdExceededGlobal = false;
	var callTreeTemplate = Handlebars.compile($("#stagemonitor-calltree-template").html());
	var metricsTemplate = Handlebars.compile($("#stagemonitor-request-template").html());

	var processRequestsMetrics = function(requestData) {
		var exceededThreshold = function(key, value) {
			switch(key) {
				case "executionCountDb":
					return value > localStorage.getItem("stagemonitor-configuration-db-count-threshold");
				case "executionTime":
					return value > localStorage.getItem("stagemonitor-configuration-execution-threshold-milliseconds");
				case "error":
					return value && localStorage.getItem("stagemonitor-configuration-notify-on-error") != "false";
				default:
					return false;
			}
		};

		var commonNames = {
			"name": {name: "Request name", description: "Usecase / Request verb and path"},
			"executionTime": {name: "Execution time in ms", description: "The time in ms it took to process the request in the server."},
			"executionTimeDb": {name: "Execution time for SQL-Queries in ms", description: ""},
			"executionTimeCpu":  {name: "Execution time for the CPU", description: "The amount of time in ms it took the CPU to process the request."},
			"executionCountDb": {name: "Number of SQL-Queries", description: "The number of SQL-Queries. Lower is better."},
			"error": {name: "Error", description: "true, if there was an error while processing the request, false otherwise."},
			"exceptionClass": {name: "Exception class", description: "The class of the thrown exception. (Only present, if there was a exception)"},
			"exceptionMessage": {name: "Exception message", description: "The message of the thrown exception. (Only present, if there was a exception)"},
			"exceptionStackTrace": {name: "Exception stack trace", description: ""},
			"stackTrace": {name: "Stacktrace", descrption: "The full stack trace of the thrown exception. (Only present, if there was a exception)"},
			"parameter": {name: "Parameters", description: "The query sting of the request. You can obfuscate sensitive parameters."},
			"clientIp": {name: "Client IP", description: "The IP of the client who initiated the HTTP request."},
			"url": {name: "URL", description: "The requested URL."},
			"statusCode": {name: "Status code", description: "The HTTP status code of a request."},
			"method": {name: "Method", description: "The HTTP method of the request."},
			"@timestamp": {name: "Timestamp", description: "The date and time the request entered the server."},
			"@application": {name: "Application name", description: "The name of the application that handled the request. This value is obtained from the display-name of web.xml. Alternatively, you can use the stagemonitor.applicationName property of the stagemonitor.properties configuration file."},
			"@host": {name: "Host accessing", description: "The name of the host of the server that handled the request."},
			"@instance": {name: "Instance", description: "The name of the instance of the application that handled the request. The instance name is useful, if you have different environments for the same application (maybe even on the same host). However, it leads to errors if you have a application with the same instance name on the same host. By default, the instance name is the domain name of the server and it is obtained from the first incoming request. You can also choose to set a fixed instance name."}
		};
		var excludedProperties = ["callStackJson", "headers", "userAgent"];
		var metrics = [];
		for(var key in requestData) {
			var isKeyIncluded = excludedProperties.indexOf(key) === -1;
			if(isKeyIncluded) {
				var nameAndDescription = commonNames[key] || {name: key, description: ""};
				var thresholdExceeded = exceededThreshold(key, requestData[key])
				if (thresholdExceeded) {
					thresholdExceededGlobal = true;
				}

				metrics.push({
					key: key,
					name: nameAndDescription.name,
					description: nameAndDescription.description,
					value: requestData[key].toString(),
					exceededThreshold: thresholdExceeded
				});
			}
		}
		return {
			metrics: metrics,
			userAgent: requestData["userAgent"],
			headers: requestData["headers"]
		};
	};
	var processCallTree = function() {
		if(arguments.length === 1) {
			// first call
			var rootCallTreeArray = arguments[0];
			var parentId = null;
			var rootId = 1;
			var rootCallTreeEntry = rootCallTreeArray[0];
			var totalExecutionTimeInMs = rootCallTreeEntry.executionTime / 1000 / 1000;
			var callTreeRows = [];
			processCallTree(rootCallTreeArray, parentId, rootId, totalExecutionTimeInMs, callTreeRows);
			return callTreeRows;
		} else {
			// recursion
			var callArray = arguments[0],
				parentId = arguments[1],
				myId = arguments[2],
				totalExecutionTimeInMs = arguments[3],
				callTreeRows = arguments[4];

			for(var i = 0; i < callArray.length; i++) {
				var callData = callArray[i];

				var executionTimeInMs = Math.round(callData.executionTime / 1000 / 10) / 100;
				var selfExecutionTimeInMs = Math.round(callData.netExecutionTime / 1000 / 10) / 100;
				var executionTimePercent = (executionTimeInMs / totalExecutionTimeInMs) * 100;
				var selfExecutionTimePercent = (selfExecutionTimeInMs / totalExecutionTimeInMs) * 100;

				callTreeRows.push({
					executionTimeExceededThreshold: executionTimePercent > localStorage.getItem("stagemonitor-configuration-execution-threshold-percent"),
					parentId: parentId,
					myId: myId,
					signature: callData.signature,
					isShortened: false,
					executionTimePercent: executionTimePercent,
					executionTimeInMs: executionTimeInMs,
					selfExecutionTimePercent: selfExecutionTimePercent,
					selfExecutionTimeInMs: selfExecutionTimeInMs
				});

				myId = processCallTree(callData.children, myId, myId + 1, totalExecutionTimeInMs, callTreeRows);
			}
			return myId;
		}
	};

	window.stagemonitor = {
		initialize: function(data) {
			var renderedMetricsTemplate = metricsTemplate(processRequestsMetrics(data));
			$("#stagemonitor-request").html(renderedMetricsTemplate);
			var $calltree = $("#stagemonitor-calltree");
			$calltree.find(".glyphicon-info-sign").tooltip();

			if (data.callStackJson !== undefined) {
				var callTree = JSON.parse(data.callStackJson);
				var callTreeRows = processCallTree([callTree]);
				var renderedCallTree = callTreeTemplate({callTreeRows: callTreeRows});
				$calltree.find("tbody").html(renderedCallTree);
				$calltree.treetable({
					expandable: true,
					initialState: "expanded",
					force: true
				});
				$calltree.find("tr[data-tt-expanded='false']").each(function () {
					$("#stagemonitor-calltree").treetable("collapseNode", $(this).attr("data-tt-id"));
				});
			} else {
				$("#call-stack-tab").hide();
				$("#stagemonitor-home").hide();
				$("#request-tab").addClass('active');
				$("#stagemonitor-request").addClass('active')
			}
		},
		thresholdExceeded: function() {
			return thresholdExceededGlobal;
		}
	};

	$("#stagemonitor-modal-close").on("click", function() {
		window.stagemonitor.closeOverlay();
	});

	$(".nav-tabs a").on("click", function (e) {
		$(this).tab('show');
		return false;
	});

	$("#stagemonitor-configuration-save").on("click", function() {
		$("input[data-stagemonitor-configuration-key]").each(function() {
			var key = $(this).attr("data-stagemonitor-configuration-key");
			if ($(this).attr("type") === "checkbox") {
				var value = $(this).prop("checked");
				localStorage.setItem(key, value);
			} else {
				var value = $(this).val();
				localStorage.setItem(key, value);
			}
		});
		return false;
	});

	$("input[data-stagemonitor-configuration-key]").each(function() {
		var key = $(this).attr("data-stagemonitor-configuration-key");
		var value = localStorage.getItem(key);
		if (value == null) {
			var defaultValue = $(this).attr("data-stagemonitor-configuration-default-value");
			value = defaultValue;
			localStorage.setItem(key, defaultValue);
		}

		if ($(this).attr("type") == "checkbox") {
			$(this).prop("checked", value == "true");
		} else {
			$(this).val(value);
		}
	});

	// spinner
	$('.stagemonitor-spinner .btn:first-of-type').on('click', function() {
		var $input = $(this).parent().prev();
		$input.val( parseInt($input.val(), 10) + 1);
		return false;
	});
	$('.stagemonitor-spinner .btn:last-of-type').on('click', function() {
		var $input = $(this).parent().prev();
		$input.val( parseInt($input.val(), 10) - 1);
		return false;
	});

	window.parent.StageMonitorLoaded();
});