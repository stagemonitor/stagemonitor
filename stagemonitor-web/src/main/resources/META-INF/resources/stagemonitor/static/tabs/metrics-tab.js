var plugins = [];

function renderMetricsTab(contextPath) {
	// TODO contextPath
	var pluginPaths = ["tabs/metrics/jvm-metrics.js", "tabs/metrics/request-metrics.js"];

	// load graphRenderer and plugin scripts
	$.when.apply(null, $.map(["tabs/metrics/graphRenderer.js"].concat(pluginPaths), loadScript)).done(function() {
		var $metricPlugins = $("#metric-plugins");
		var $sideMenu = $("#side-menu");
		$.each(plugins, function (i, plugin) {
			$sideMenu.append('<li class="plugin-link' + (i == 0 ? ' active' : '') + '">' +
				'	<a href="#' + plugin.id + '">' + plugin.label + '</a>' +
				'</li>');
			// custom invisible class, not hidden
			// because flot requires the elements not to be hidden so it knows the width and height
			$metricPlugins.append('<div id="' + plugin.id + '" class="metric-plugin invisible"></div>');
		});

		// load html of plugins
		$.when.apply(null, $.map(plugins, loadPluginHtml)).done(function() {
			// render graphs lazily when user clicks on metrics tab
			$("#metrics-tab").find("a").one('click', function () {
				$.each(plugins, function (i, plugin) {
					plugin.onHtmlInitialized && plugin.onHtmlInitialized();
				});

				var graphs = $.map(plugins, function (plugin) {
					return plugin.graphs;
				});
				graphRenderer.renderGraphs(graphs, function (metrics) {
					$.each(plugins, function (i, plugin) {
						plugin.onMetricsReceived && plugin.onMetricsReceived(metrics);
					});
				}, function () {
					// after rendering the graphs remove the custom invisible class and hide all non active plugins
					$(".metric-plugin").addClass("hidden").removeClass("invisible");
					$($(".plugin-link.active > a").attr("href")).removeClass("hidden");
				});
			});
		});

		// select a plugin from the side bar
		$sideMenu.find("a").click(function () {
			var thisLink = $(this);
			$(".plugin-link").removeClass("active");
			thisLink.parent().addClass("active");
			$(".metric-plugin").addClass("hidden");
			$(thisLink.attr("href")).removeClass("hidden");
			return false;
		});
	});

	function loadPluginHtml(plugin) {
		return $('#' + plugin.id).load(plugin.htmlPath);
	}

	function loadScript(path) {
		var result = $.Deferred(),
			script = document.createElement("script");
		script.async = "async";
		script.type = "text/javascript";
		script.src = path;
		script.onload = script.onreadystatechange = function (_, isAbort) {
			if (!script.readyState || /loaded|complete/.test(script.readyState)) {
				if (isAbort)
					result.reject();
				else
					result.resolve();
			}
		};
		script.onerror = function () { result.reject(); };
		$("head")[0].appendChild(script);
		return result.promise();
	}

}

