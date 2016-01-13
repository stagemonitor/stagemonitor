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
			calculateQueryCount(callTree);
			var callTreeRows = [];
			processCallTree(callTreeRows, [callTree], null, 1, callTree.executionTime);
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
					queryCount: callData.queryCount,
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

		function calculateQueryCount(node) {
			node.queryCount = 0;
			var childrenQueryCount = 0;
			for (var i = 0; i < node.children.length; i++) {
				var child = node.children[i];
				child.parent = node;
				calculateQueryCount(child);
				if (child.ioquery) childrenQueryCount++;
			}
			incrementQueryCountForParents(node, childrenQueryCount)
		}

		function incrementQueryCountForParents(node, count) {
			if (count == 0) return;
			node.queryCount += count;
			if (node.parent) {
				incrementQueryCountForParents(node.parent, count);
			}
		}

	});
}
