$(document).ready(function () {

	window.stagemonitor = {
		initialize: function (data, configurationSources, configurationOptions, baseUrl, contextPath, passwordSet,
							  connectionId, pathsOfWidgetMetricTabPlugins) {
			stagemonitor.requestTrace = data;
			stagemonitor.configurationSources = configurationSources;
			stagemonitor.configurationOptions = configurationOptions;
			stagemonitor.baseUrl = baseUrl;
			stagemonitor.contextPath = contextPath;
			stagemonitor.passwordSet = passwordSet;
			stagemonitor.connectionId = connectionId;
			stagemonitor.pathsOfWidgetMetricTabPlugins = pathsOfWidgetMetricTabPlugins;

			listenForAjaxRequestTraces(data, connectionId);
			renderRequestTab(data);
			renderConfigTab(configurationSources, configurationOptions, passwordSet);
			renderCallTree(data);
			try {
				renderMetricsTab();
			} catch (e) {
				console.log(e);
			}
			$(".tip").tooltip();
		},
		thresholdExceeded: false,
		renderPageLoadTime: function(data) {
			doRenderPageLoadTime(data);
			$(".tip").tooltip({html: true});
		}
	};

	$("#stagemonitor-modal-close").on("click", function () {
		window.stagemonitor.closeOverlay();
	});

	$(".nav-tabs a").on("click", function (e) {
		$(this).tab('show');
		return false;
	});

	// serialize checkboxes as true/false
	var originalSerializeArray = $.fn.serializeArray;
	$.fn.extend({
		serializeArray: function () {
			var brokenSerialization = originalSerializeArray.apply(this);
			var checkboxValues = $(this).find('input[type=checkbox]').map(function () {
				return { 'name': this.name, 'value': this.checked };
			}).get();
			var checkboxKeys = $.map(checkboxValues, function (element) {
				return element.name;
			});
			var withoutCheckboxes = $.grep(brokenSerialization, function (element) {
				return $.inArray(element.name, checkboxKeys) == -1;
			});

			return $.merge(withoutCheckboxes, checkboxValues);
		}
	});

	// toast notification settings
	$.growl(false, {
		allow_dismiss: true,
		placement: {
			from: "top",
			align: "center"
		},
		mouse_over: "pause",
		delay: 5000
	});

	try {
		window.parent.StagemonitorLoaded();
	} catch (e){}

});