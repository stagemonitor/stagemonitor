function renderRequestTab(data) {
	var $requestTab = $("#request-tab");
	if (!data) {
		$requestTab.hide();
		// show config tab as fallback
		if ($("#call-stack-tab").hasClass('active') || $requestTab.hasClass('active')) {
			$("#config-tab").addClass('active');
			$("#stagemonitor-configuration").addClass('active');
		}
		return;
	} else {
		$requestTab.show();
	}

	var thresholdExceededGlobal = false;
	var requestsMetrics = processRequestsMetrics(data);

	$.get(stagemonitor.contextPath + "/stagemonitor/static/tabs/request-tab.html", function (template) {
		var metricsTemplate = Handlebars.compile($(template).html());
		var renderedMetricsTemplate = metricsTemplate(requestsMetrics);
		var $stagemonitorRequest = $("#stagemonitor-request");
		$stagemonitorRequest.html(renderedMetricsTemplate);
		$(".tip").tooltip({html: true});
	});


	function processRequestsMetrics(requestData) {
		var exceededThreshold = function (key, value) {
			switch (key) {
				case "executionCountDb":
					return value > localStorage.getItem("widget-settings-db-count-threshold");
				case "executionTime":
					return value > localStorage.getItem("widget-settings-execution-threshold-milliseconds");
				case "error":
					return value && localStorage.getItem("widget-settings-notify-on-error") != "false";
				default:
					return false;
			}
		};

		var commonNames = {
			"name": {name: "Request name", description: "Usecase / Request verb and path"},
			"executionTime": {name: "Server execution time in ms", description: "The time in ms it took to process the request in the server."},
			"executionTimeDb": {name: "Execution time for SQL-Queries in ms", description: ""},
			"executionTimeCpu": {name: "Execution time for the CPU", description: "The amount of time in ms it took the CPU to process the request."},
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
		for (var key in requestData) {
			var isKeyIncluded = excludedProperties.indexOf(key) === -1;
			if (isKeyIncluded) {
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
	}

	stagemonitor.thresholdExceeded |= thresholdExceededGlobal;
}

function doRenderPageLoadTime() {
	var data = stagemonitor.pageLoadTimeData;
	if (!data) {
		return;
	}
	var thresholdMs = localStorage.getItem("widget-settings-execution-threshold-milliseconds");
	var thresholdExceeded = data.totalPageLoadTime > thresholdMs;

	$.get(stagemonitor.contextPath + "/stagemonitor/static/tabs/request-tab-page-load-time.html", function (template) {
		var pageLoadTimeTemplate = Handlebars.compile($(template).html());
		var model = {
			networkMs: data.timeToFirstByte - data.serverTime,
			networkPercent: (((data.timeToFirstByte - data.serverTime) / data.totalPageLoadTime) * 100).toFixed(2),
			serverMs: data.serverTime,
			serverPercent: ((data.serverTime / data.totalPageLoadTime) * 100).toFixed(2),
			serverThresholdExceeded: data.serverTime > thresholdMs,
			domProcessingMs: data.domProcessing,
			domProcessingPercent: ((data.domProcessing / data.totalPageLoadTime) * 100).toFixed(2),
			pageRenderingMs: data.pageRendering,
			pageRenderingPercent: ((data.pageRendering / data.totalPageLoadTime) * 100).toFixed(2),
			totalMs: data.totalPageLoadTime,
			totalThresholdExceeded: thresholdExceeded
		};
		model["pageRenderingPercent"] = (100 - model.networkPercent - model.serverPercent - model.domProcessingPercent).toFixed(2);
		var renderedMetricsTemplate = pageLoadTimeTemplate(model);
		var $stagemonitorRequest = $("#stagemonitor-request");
		$stagemonitorRequest.prepend(renderedMetricsTemplate);
		$(".tip").tooltip({html: true});
	});

	stagemonitor.thresholdExceeded |= thresholdExceeded;
}