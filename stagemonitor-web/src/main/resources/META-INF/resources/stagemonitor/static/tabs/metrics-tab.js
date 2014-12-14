function renderMetricsTab() {
	window.plugins = [];
	var tickMs = 1000;

	// TODO contextPath
	var pluginPaths = ["tabs/metrics/jvm-metrics.js", "tabs/metrics/request-metrics.js"];

	// load graphRenderer and plugin scripts
	utils.loadScripts(["tabs/metrics/graphRenderer.js", "tabs/metrics/timerTableRenderer.js"].concat(pluginPaths), function() {
		var $metricPlugins = $("#metric-plugins");
		var $sideMenu = $("#side-menu");
		plugins.sort(function (p1, p2) {
			return p1.label.localeCompare(p2.label);
		});
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
				renderAllGraphs();
				renderAllTimerTables();
				setInterval(function () {
					getMetricsFromServer(function (metrics) {
						$.each(plugins, function (i, plugin) {
							plugin.onMetricsReceived && plugin.onMetricsReceived(metrics);
							timerTableRenderer.onMetricsReceived(metrics);
							graphRenderer.onMetricsReceived(metrics);
						});
					});
				}, tickMs);
			});
		});
		initSideMenu();
	});

	function renderAllGraphs() {
		var graphs = $.map(plugins, function (plugin) {
			return plugin.graphs;
		});
		getMetricsFromServer(function (metrics) {
			graphRenderer.renderGraphs(graphs, metrics, function () {
				// after rendering the graphs remove the custom invisible class and hide all non active plugins
				$(".metric-plugin").addClass("hidden").removeClass("invisible");
				$($(".plugin-link.active > a").attr("href")).removeClass("hidden");
			});
		});
	}

	function renderAllTimerTables() {
		var tables = $.map(plugins, function (plugin) {
			return plugin.timerTable;
		});
		$.each(tables, function (i, table) {
			timerTableRenderer.init(table);
		});
	}

	// select a plugin from the side bar
	function initSideMenu() {
		$("#side-menu").find("a").click(function () {
			var thisLink = $(this);
			$(".plugin-link").removeClass("active");
			thisLink.parent().addClass("active");
			$(".metric-plugin").addClass("hidden");
			$(thisLink.attr("href")).removeClass("hidden");
			return false;
		});
	}

	function loadPluginHtml(plugin) {
		return $('#' + plugin.id).load(plugin.htmlPath);
	}

	function getMetricsFromServer(onMetricsReceived) {
//		$.getJSON(contextPath + "/stagemonitor/metrics", function(data) {
		$.getJSON("http://localhost:8880/petclinic/stagemonitor/metrics", function (metrics) {
			var date = new Date();
			metrics['timestamp'] = date.getTime();
			onMetricsReceived(metrics);
		});
	}

}

