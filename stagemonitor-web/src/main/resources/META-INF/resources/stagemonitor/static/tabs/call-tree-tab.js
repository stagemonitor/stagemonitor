function renderCallTree(data) {
	$.get("tabs/call-tree-tab.html", function (template) {
		var callTreeTemplate = Handlebars.compile($(template).html());

		if (data.callStackJson !== undefined) {
			var callTree = JSON.parse(data.callStackJson);
			var callTreeRows = [];
			processCallTree(callTreeRows, [callTree], null, 1, callTree.executionTime);
			$("#stagemonitor-home").html("");
			$("#stagemonitor-home").html(callTreeTemplate({callTreeRows: callTreeRows}));
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

		} else {
			$("#call-stack-tab").hide();
			$("#stagemonitor-home").hide();
			$("#request-tab").addClass('active');
			$("#stagemonitor-request").addClass('active')
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
					signature: callData.signature,
					isShortened: false,
					executionTimePercent: executionTimePercent,
					executionTimeInMs: executionTimeInMs,
					selfExecutionTimePercent: selfExecutionTimePercent,
					selfExecutionTimeInMs: selfExecutionTimeInMs
				});

				myId = processCallTree(callTreeRows, callData.children, myId, myId + 1, totalExecutionTimeInNs);
			}
			return myId;
		}
	});
}
