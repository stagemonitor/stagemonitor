$(document).ready(function () {
	window.tabPlugins = [];

	window.stagemonitor = {
		initialize: function (data, configurationSources, configurationOptions, baseUrl, contextPath, passwordSet, connectionId, pathsOfWidgetMetricTabPlugins) {
			try {

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
				loadTabPlugins();
				$(".tip").tooltip();
			} catch (e) {
				console.log(e);
			}
		},
		thresholdExceeded: false,
		renderPageLoadTime: function (data) {
			doRenderPageLoadTime(data);
			$(".tip").tooltip({html: true});
		}
	};

	function loadTabPlugins() {
		$.each(stagemonitor.pathsOfTabPlugins, function (i, tabPluginPath) {
			utils.loadScripts([tabPluginPath + ".js"], function () {
				$.each(tabPlugins, function (i, tabPlugin) {
					try {
						$("#tab-list").append('<li id="' + tabPlugin.tabId + '">' +
							'	<a href="#' + tabPlugin.tabContentId + '" role="tab" data-toggle="tab" class="tip" data-placement="bottom"' +
							'		title="' + (tabPlugin.tooltip || '') + '">' +
							tabPlugin.label +
							'	</a>' +
							'</li>');

						$("#tab-content").append('<div class="tab-pane" id="' + tabPlugin.tabContentId + '"></div>');

						$.get(stagemonitor.contextPath + tabPluginPath + ".html", function (html) {
							tabPlugin.renderTab(html);
						});
					} catch (e) {
						console.log(e);
					}
				});
			});
		});
	}

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

	try {
		window.parent.StagemonitorLoaded();
	} catch (e) {
	}

	$("#tab-content").on("click", ".branch", function (e) {
		if (!$(e.target).hasClass("expander")) {
			var treeTableNodeId = $(this).data("tt-id");
			$("#stagemonitor-calltree").treetable("node", treeTableNodeId).toggle();
		}
	});

});