var tableRenderer = (function () {
	var metricTables = [];
	var selectedPluginId;
	$.fn.dataTableExt.sErrMode = 'throw';

	return {
		renderTables: function (plugins) {
			$.each(plugins, function (i, plugin) {
				if (plugin.table) {
					renderTable(plugin.id, plugin.table);
				}
			});
		},
		onMetricsReceived: function (metrics) {
			$.each(metricTables, function (i, metricTable) {
				// only update table if it is visible
				// or it is the first invocation of onMetricsReceived so that the graphs can be populated
				if (!metricTable.alreadyReceivedData || metricTable.pluginId == selectedPluginId) {
					metricTable.alreadyReceivedData = true;
					updateTable(metrics, metricTable);
				}
			});
		},
		onPluginSelected: function(pluginId) {
			selectedPluginId = pluginId;
		}
	};

	function renderTable(pluginId, metricTable) {
		metricTable.pluginId = pluginId;
		var aoColumns = [{sTitle: metricTable.nameLabel, mData: "name"}];
		$.each(metricTable.columns, function (columnIndex, column) {
			aoColumns.push({sTitle: column.title, mData: columnIndex});
		});
		metricTables.push(metricTable);
		metricTable.table = $(metricTable.bindto).dataTable({
			"bLengthChange": false,
			"aoColumns": aoColumns
		});
		metricTable.table.find('tbody').on('click', 'tr', function () {
			if (!$(this).hasClass('selected')) {
				// select
				metricTable.table.$('tr.selected').removeClass('selected');
				$(this).addClass('selected');
				metricTable.selectedName = metricTable.table.fnGetData(metricTable.table.fnGetPosition(this)).name;
				onRowSelected(metricTable);
			} else {
				// deselect
				metricTable.selectedName = null;
				$(this).removeClass('selected');
				onRowDeselected(metricTable);
			}
		});
	}

	function updateTable(metrics, metricTable) {
		var data = getData(metrics, metricTable);
		if (data.length > 0) {
			metricTable.table.fnClearTable(false);
			metricTable.table.fnAddData(data, false);
			metricTable.table.DataTable().draw(false);
		}
		restoreSelectedRow(metricTable);
	}

	function getData(metrics, metricTable) {
		var dataByRowName = {};
		$.each(metricTable.columns, function (columnIndex, column) {
			for (var i = 0; i < metrics.length; i++) {
				var metric = metrics[i];
				if (utils.matches(metric, column.metricMatcher)) {
					var rowName = metric.tags[column.groupBy] || utils.metricAsString(metric, column.metric);
					dataByRowName[rowName] = dataByRowName[rowName] || {};
					dataByRowName[rowName].name = rowName;
					var value = metric.values[column.metric];
					if (isNaN(value)) {
						value = 0;
					}
					dataByRowName[rowName][columnIndex] = +(value).toFixed(2);
					metricTable.rows = metricTable.rows || {};
					if (!metricTable.rows[rowName]) {
						onNewRow(metricTable, rowName, metrics);
						metricTable.rows[rowName] = true;
					}
				}
			}
		});


		var valuesArray = utils.objectToValuesArray(dataByRowName);
		fillMissingData(valuesArray, metricTable);
		return  valuesArray;
	}

	function fillMissingData(valuesArray, metricTable) {
		for (var i = 0; i < valuesArray.length; i++) {
			var metricsForRow = valuesArray[i];
			$.each(metricTable.columns, function (columnIndex, column) {
				if (typeof metricsForRow[columnIndex] === 'undefined') {
					metricsForRow[columnIndex] = 0;
				}
			});
		}
	}

	function restoreSelectedRow(timerTable) {
		$(timerTable.table.fnSettings().aoData).each(function () {
			var requestName = timerTable.table.fnGetData(this.nTr).name;
			if (requestName == timerTable.selectedName) {
				$(this.nTr).addClass('selected');
			}
		});
	}

	function onRowSelected(timerTable) {
		if (timerTable.graphTemplates && timerTable.graphTemplates.templates) {
			$.each(timerTable.graphTemplates.templates, function (i, graphTemplate) {
				graphRenderer.disableGraphsBoundTo(graphTemplate.template.bindto);
			});
			$.each(timerTable.graphTemplates.templates, function (i, graphTemplate) {
				graphRenderer.activateGraph(graphTemplate.graphs[timerTable.selectedName]);
			});
		}
	}

	function onRowDeselected(timerTable) {
		if (timerTable.graphTemplates && timerTable.graphTemplates.templates) {
			$.each(timerTable.graphTemplates.templates, function (i, graphTemplate) {
				graphRenderer.disableGraphsBoundTo(graphTemplate.template.bindto);
			});
			if (timerTable.graphTemplates.defaultRowSelection) {
				$.each(timerTable.graphTemplates.templates, function (i, graphTemplate) {
					graphRenderer.activateGraph(graphTemplate.graphs[timerTable.graphTemplates.defaultRowSelection]);
				});
			}
		}
	}

	function onNewRow(table, rowName, metrics) {
		addGraphsFromTemplates(table, rowName, metrics);
		if (table.graphTemplates && table.graphTemplates.templates && table.graphTemplates.defaultRowSelection) {
			// don't Regex-quote the defaultRowSelection as it may be a regex
			addGraphsFromTemplates(table, table.graphTemplates.defaultRowSelection, metrics);
		}
	}

	function addGraphsFromTemplates(table, rowName, metrics) {
		if (table.graphTemplates && table.graphTemplates.templates) {
			$.each(table.graphTemplates.templates, function (i, graphTemplate) {
				graphTemplate.graphs = graphTemplate.graphs || {};
				if (!graphTemplate.graphs[rowName]) {
					graphTemplate.graphs[rowName] = processGraphTemplate(graphTemplate, rowName, table);
					graphRenderer.addGraph(table.pluginId, graphTemplate.graphs[rowName], metrics);
				}
			});
		}
	}

	function processGraphTemplate(graphTemplate, rowName, table) {
		// TODO quoteRowName
		var template = JSON.parse(JSON.stringify(graphTemplate.template));
		template.disabled = rowName != table.graphTemplates.defaultRowSelection;
		for (var j = 0; j < template.columns.length; j++) {
			var column = template.columns[j];
			$.each(column.metricMatcher.tags, function (tag, value) {
				column.metricMatcher.tags[tag] = value.replace("${rowName}", rowName);
			});
		}
		return template;
	}

})();
