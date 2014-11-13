$(document).ready(function () {

	var thresholdExceededGlobal = false;


	window.stagemonitor = {
		initialize: function (data, configurationSources, configurationOptions, contextPathPrefix, passwordSet, connectionId) {
			listenForAjaxRequestTraces(data, connectionId, contextPathPrefix);
			thresholdExceededGlobal = renderRequestTab(data);
			renderConfigTab(configurationSources, configurationOptions, passwordSet, contextPathPrefix);
			renderCallTree(data);
			$(".tip").tooltip();
		},
		thresholdExceeded: function () {
			return thresholdExceededGlobal;
		},
		renderPageLoadTime: function(data) {
			var thresholdExceeded = doRenderPageLoadTime(data);
			if (thresholdExceeded) {
				thresholdExceededGlobal = true;
			}

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

	window.parent.StagemonitorLoaded();
});