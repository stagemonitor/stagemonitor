var tableRenderer = (function () {
	var metricTables = [];
	var selectedPluginId;
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
				if (!metricTable.alreadyReceivedData || metricTable.pluginId == selectedPluginId) {
					metricTable.alreadyReceivedData = true;
					var data = getData(metrics, metricTable);
					metricTable.table.fnClearTable(false);
					metricTable.table.fnAddData(data, false);
					metricTable.table.DataTable().draw(false);
					restoreSelectedRow(metricTable);
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
		$.each(metricTable.columns, function (i, column) {
			aoColumns.push({sTitle: column.title, mData: column.uniqueName});
		});
		metricTables.push(metricTable);
		metricTable.table = $(metricTable.bindto).dataTable({
			"bJQueryUI": true,
			"sPaginationType": "full_numbers",
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

	function getData(metrics, metricTable) {
		var dataByRowName = {};
		$.each(metricTable.columns, function (i, column) {
			var metricCategory = metrics[column.metricCategory];
			for (metricPath in metricCategory) {
				var match = column.metricPathRegex.exec(metricPath);
				if (match) {
					var rowName = match[1] || match[0];
					dataByRowName[rowName] = dataByRowName[rowName] || {};
					dataByRowName[rowName].name = rowName;
					dataByRowName[rowName][column.uniqueName] = metricCategory[metricPath][column.metric].toFixed(2);
					metricTable.rows = metricTable.rows || {};
					if (!metricTable.rows[rowName]) {
						onNewRow(metricTable, rowName, metrics);
						metricTable.rows[rowName] = true;
					}
				}
			}
		});

		var data = [];
		for(var columnName in dataByRowName) {
			data.push(dataByRowName[columnName]);
		}
		return data;
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
		addGraphsFromTemplates(table, rowName, metrics, true);
		if (table.graphTemplates && table.graphTemplates.templates && table.graphTemplates.defaultRowSelection) {
			addGraphsFromTemplates(table, table.graphTemplates.defaultRowSelection, metrics, false);
		}
	}

	function addGraphsFromTemplates(table, rowName, metrics, quoteRowName) {
		if (table.graphTemplates && table.graphTemplates.templates) {
			$.each(table.graphTemplates.templates, function (i, graphTemplate) {
				graphTemplate.graphs = graphTemplate.graphs || {};
				if (!graphTemplate.graphs[rowName]) {
					graphTemplate.graphs[rowName] = processGraphTemplate(graphTemplate, rowName, table, quoteRowName);
					graphRenderer.addGraph(table.pluginId, graphTemplate.graphs[rowName], metrics);
				}
			});
		}
	}

	function processGraphTemplate(graphTemplate, rowName, table, quoteRowName) {
		var template = JSON.parse(JSON.stringify(graphTemplate.template));
		template.disabled = rowName != table.graphTemplates.defaultRowSelection;
		for (var j = 0; j < template.columns.length; j++) {
			var column = template.columns[j];
			column[1] = column[1].replace("${rowName}", quoteRowName ? RegExp.quote(rowName) : rowName);
		}
		return template;
	}

})();