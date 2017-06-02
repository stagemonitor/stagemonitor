function renderMetricsTab() {
	window.plugins = [];
	var requiredMetricNames = [];
	var tickMs = +localStorage.getItem("widget-settings-metrics-refresh") * 1000;
	var activePluginId;

	var scripts = $.map(stagemonitor.pathsOfWidgetMetricTabPlugins, function (path) {
		return path + ".js"
	});

	utils.loadScripts(scripts, function () {
		plugins.sort(function (p1, p2) {
			return p1.label.localeCompare(p2.label);
		});
		$.each(plugins, function (i, plugin) {
			var activeClass = '';
			if (i == 0) {
				onPluginSelected(plugin.id);
				activeClass = ' active';
			}
			$("#side-menu").append('<li class="plugin-link' + activeClass + '">' +
				'	<a href="#' + plugin.id + '">' + plugin.label + '</a>' +
				'</li>');
			// custom invisible class, not hidden
			// because flot requires the elements not to be hidden as it needs to know the width and height
			$("#metric-plugins").append('<div id="' + plugin.id + '" class="metric-plugin invisible"></div>');
			registerAllMetricMatchers(plugin);
		});

		loadHtmlOfPlugins();
		initSideMenu();
	});

	function registerAllMetricMatchers(plugin) {
		if (plugin.table) {
			for (var j = 0; j < plugin.table.columns.length; j++) {
				registerMetricMatcher(plugin.table.columns[j].metricMatcher);
			}
			if (plugin.table.graphTemplates) {
				for (var j = 0; j < plugin.table.graphTemplates.length; j++) {
					registerMetricMatcher(plugin.table.graphTemplates[j].template.metricMatcher);
				}
			}
		}
		if (plugin.graphs) {
			for (var j = 0; j < plugin.graphs.length; j++) {
				var graph = plugin.graphs[j];
				for (var k = 0; k < graph.columns.length; k++) {
					registerMetricMatcher(graph.columns[k].metricMatcher);
				}
			}
		}
	}

	function registerMetricMatcher(metricMatcher) {
		if (metricMatcher) {
			var metricName = metricMatcher.name;
			if (requiredMetricNames.indexOf(metricName) == -1) {
				requiredMetricNames.push(metricName);
			}
		}
	}

	function loadHtmlOfPlugins() {
		$.when.apply(null, $.map(stagemonitor.pathsOfWidgetMetricTabPlugins, loadPluginHtml)).done(function () {
			// render graphs lazily when user clicks on metrics tab
			if ($("#stagemonitor-metrics").hasClass("active")) {
				renderGraphs();
			} else {
				$("#metrics-tab").find("a").one('click', function () {
					renderGraphs();
				});
			}

			function renderGraphs() {
				$.each(plugins, function (i, plugin) {
					plugin.onHtmlInitialized && plugin.onHtmlInitialized();
				});
				renderAllTimerTables();
				renderAllGraphs();
				setInterval(function () {
					getMetricsFromServer(onMetricsReceived);
				}, tickMs);
			}
		});
	}

	function onMetricsReceived(metrics) {
		// give the plugins a chance to modify the metrics
		$.each(plugins, function (i, plugin) {
			plugin.onMetricsReceived && plugin.onMetricsReceived(metrics);
		});
		tableRenderer.onMetricsReceived(metrics);
		graphRenderer.onMetricsReceived(metrics);
	}

	function renderAllGraphs() {
		getMetricsFromServer(function (metrics) {
			onMetricsReceived(metrics);
			graphRenderer.renderGraphs(plugins, metrics, function () {
				// after rendering the graphs remove the custom invisible class and hide all non active plugins
				$(".metric-plugin").addClass("hidden").removeClass("invisible");
				$($(".plugin-link.active > a").attr("href")).removeClass("hidden");
			});
		});
	}

	function renderAllTimerTables() {
		tableRenderer.renderTables(plugins);
	}

	function onPluginSelected(pluginId) {
		if (activePluginId != pluginId) {
			activePluginId = pluginId;
			graphRenderer.onPluginSelected(pluginId);
			tableRenderer.onPluginSelected(pluginId);
		}
	}

	function initSideMenu() {
		// on selecting a plugin from the side bar
		$("#side-menu").find("a").click(function () {
			var thisLink = $(this);
			$(".plugin-link").removeClass("active");
			thisLink.parent().addClass("active");
			$(".metric-plugin").addClass("hidden");
			var pluginId = thisLink.attr("href");
			$(pluginId).removeClass("hidden");
			onPluginSelected(pluginId.replace('#', ''));
			return false;
		});
	}

	function loadPluginHtml(pluginPath) {
		return $.get(pluginPath + ".html", function (data) {
			$('#' + getPluginId(pluginPath)).html(data);
		});
	}

	function getMetricsFromServer(onMetricsReceived) {
		$.getJSON(stagemonitor.baseUrl + "/stagemonitor/metrics", {
			metricNames: requiredMetricNames
		})
			.done(function (metrics) {
				var date = new Date();
				metrics['timestamp'] = date.getTime();
				onMetricsReceived(metrics);
			});
	}

	function getPluginId(pluginPath) {
		return pluginPath.substring(pluginPath.lastIndexOf("/") + 1, pluginPath.length)
	}

}

