function renderMetricsTab() {
	window.plugins = [];
	var tickMs = 1000;

	var scripts = $.map(stagemonitor.pathsOfWidgetMetricTabPlugins, function (path) {
		return path + ".js"
	});
	utils.loadScripts(scripts, function() {
		plugins.sort(function (p1, p2) {
			return p1.label.localeCompare(p2.label);
		});
		$.each(plugins, function (i, plugin) {
			$("#side-menu").append('<li class="plugin-link' + (i == 0 ? ' active' : '') + '">' +
				'	<a href="#' + plugin.id + '">' + plugin.label + '</a>' +
				'</li>');
			// custom invisible class, not hidden
			// because flot requires the elements not to be hidden as it needs to know the width and height
			$("#metric-plugins").append('<div id="' + plugin.id + '" class="metric-plugin invisible"></div>');
		});

		loadHtmlOfPlugins();
		initSideMenu();
	});

	function loadHtmlOfPlugins() {
		$.when.apply(null, $.map(stagemonitor.pathsOfWidgetMetricTabPlugins, loadPluginHtml)).done(function () {
			// render graphs lazily when user clicks on metrics tab
			$("#metrics-tab").find("a").one('click', function () {
				$.each(plugins, function (i, plugin) {
					plugin.onHtmlInitialized && plugin.onHtmlInitialized();
				});
				renderAllGraphs();
				renderAllTimerTables();
				setInterval(function () {
					getMetricsFromServer(onMetricsReceived);
				}, tickMs);
			});
		});
	}

	function onMetricsReceived(metrics) {
		tableRenderer.onMetricsReceived(metrics);
		graphRenderer.onMetricsReceived(metrics);
		$.each(plugins, function (i, plugin) {
			plugin.onMetricsReceived && plugin.onMetricsReceived(metrics);
		});
	}

	function renderAllGraphs() {
		var graphs = $.map(plugins, function (plugin) {
			return plugin.graphs;
		});
		getMetricsFromServer(function (metrics) {
			// give the plugins a chance to modify the metrics
			$.each(plugins, function (i, plugin) {
				plugin.onMetricsReceived && plugin.onMetricsReceived(metrics);
			});
			graphRenderer.renderGraphs(graphs, metrics, function () {
				// after rendering the graphs remove the custom invisible class and hide all non active plugins
				$(".metric-plugin").addClass("hidden").removeClass("invisible");
				$($(".plugin-link.active > a").attr("href")).removeClass("hidden");
			});
		});
	}

	function renderAllTimerTables() {
		var tables = $.map(plugins, function (plugin) {
			return plugin.table;
		});
		$.each(tables, function (i, table) {
			tableRenderer.init(table);
		});
	}

	function initSideMenu() {
		// on selecting a plugin from the side bar
		$("#side-menu").find("a").click(function () {
			var thisLink = $(this);
			$(".plugin-link").removeClass("active");
			thisLink.parent().addClass("active");
			$(".metric-plugin").addClass("hidden");
			$(thisLink.attr("href")).removeClass("hidden");
			return false;
		});
	}

	function loadPluginHtml(pluginPath) {
		return $('#' + getPluginId(pluginPath)).load(pluginPath + ".html");
	}

	function getMetricsFromServer(onMetricsReceived) {
		$.getJSON(stagemonitor.baseUrl + "/stagemonitor/metrics", function (metrics) {
			var date = new Date();
			metrics['timestamp'] = date.getTime();
			onMetricsReceived(metrics);
		});
	}

	function getPluginId(pluginPath) {
		return pluginPath.substring(pluginPath.lastIndexOf("/") + 1, pluginPath.length)
	}

}

