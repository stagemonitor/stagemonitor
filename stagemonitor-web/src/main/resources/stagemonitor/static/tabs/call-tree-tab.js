function setCallTree(callTree) {
	stagemonitor.callTree = callTree;
}

function renderCallTree(callTree) {
	if (callTree) {
		setCallTree(callTree);
	}
	if (stagemonitor.callTree === stagemonitor.renderedCallTree) {
		return;
	}
	$.get(stagemonitor.contextPath + "/stagemonitor/static/tabs/call-tree-tab.html", function (template) {
		var callTreeTemplate = Handlebars.compile($(template).html());

		var $stagemonitorHome = $("#stagemonitor-home");
		var $callStackTab = $("#call-stack-tab");
		if (stagemonitor.callTree && stagemonitor.callTree.callStackJson !== undefined) {
			$callStackTab.show();
			var callTree = JSON.parse(stagemonitor.callTree.callStackJson);
			var callTreeRows = [];
			processCallTree(callTreeRows, [callTree], null, 1, callTree.executionTime);
			assignQueryCountForEachCallTreeRow(callTreeRows);
			$stagemonitorHome.html("");
			$stagemonitorHome.html(callTreeTemplate({callTreeRows: callTreeRows}));
			$(".tip").tooltip();
			var $calltree = $("#stagemonitor-calltree");
			$calltree.treetable({
				expandable: true,
				force: true,
				indent: 25,
				initialState: "expanded",
				expanderTemplate: "<a class='expander' href='#'>&nbsp;</a>"
			});
			$calltree.find("tr[data-tt-expanded='false']").each(function () {
				$calltree.treetable("collapseNode", $(this).attr("data-tt-id"));
			});
			stagemonitor.renderedCallTree = stagemonitor.callTree;

		} else {
			$callStackTab.hide();
			if ($callStackTab.hasClass('active')) {
				$("#request-tab").addClass('active');
				$("#stagemonitor-request").addClass('active')
			}
		}

		function processCallTree(callTreeRows, callArray, parentId, myId, totalExecutionTimeInNs) {
			var thresholdPercent = localStorage.getItem("widget-settings-execution-threshold-percent");
			var totalExecutionTimeInMs = totalExecutionTimeInNs / 1000 / 1000;
			for (var i = 0; i < callArray.length; i++) {
				var callData = callArray[i];

				var executionTimeInMs = Math.round(callData.executionTime / 1000 / 10) / 100;
				var selfExecutionTimeInMs = Math.round(callData.netExecutionTime / 1000 / 10) / 100;
				var executionTimePercent = (executionTimeInMs / totalExecutionTimeInMs) * 100;
				var selfExecutionTimePercent = (selfExecutionTimeInMs / totalExecutionTimeInMs) * 100;
				var anyChildExceedsThreshold = $.grep(callData.children,function (e) {
					return (e.executionTime / totalExecutionTimeInNs * 100) > thresholdPercent;
				}).length > 0;

				callTreeRows.push({
					executionTimeExceededThreshold: executionTimePercent > thresholdPercent,
					anyChildExceedsThreshold: anyChildExceedsThreshold,
					parentId: parentId,
					myId: myId,
					shortSignature: callData.shortSignature,
					signature: callData.signature,
					ioQuery: callData.ioquery,
					executionTimePercent: executionTimePercent,
					executionTimeInNs: callData.executionTime,
					executionTimeInMs: executionTimeInMs,
					selfExecutionTimePercent: selfExecutionTimePercent,
					selfExecutionTimeInMs: selfExecutionTimeInMs
				});

				myId = processCallTree(callTreeRows, callData.children, myId, myId + 1, totalExecutionTimeInNs);
			}
			return myId;
		}

		/**
		 * For each callTreeRow count all queries of the underlying child rows.
		 *
		 * @param callTreeRows The "processed" callTree from function processCallTree
		 */
		function assignQueryCountForEachCallTreeRow(callTreeRows) {
			for (var i = 0; i < callTreeRows.length; i++) {
				var queryCount = 0;
				for (var q = i + 1; q < callTreeRows.length; q++) {
					if (callTreeRows[q].parentId >= callTreeRows[i].myId) {
						if (callTreeRows[q].ioQuery) {
							queryCount++;
						}
					} else {
						break;
					}
				}
				callTreeRows[i].queryCount = queryCount;
			}
		}

	});
}
