function renderCallTree(data) {
	$.get("tabs/call-tree-tab.html", function (template) {
		var callTreeTemplate = Handlebars.compile($(template).html());

		var $stagemonitorHome = $("#stagemonitor-home");
		var $callStackTab = $("#call-stack-tab");
		if (data && data.callStackJson !== undefined) {
			$callStackTab.show();
			var callTree = JSON.parse(data.callStackJson);
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

				var shortSignature = getShortSignature(callData.signature);
				callTreeRows.push({
					executionTimeExceededThreshold: executionTimePercent > thresholdPercent,
					anyChildExceedsThreshold: anyChildExceedsThreshold,
					parentId: parentId,
					myId: myId,
					shortSignature: shortSignature,
					signature: callData.signature,
					isShortened: shortSignature != callData.signature,
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

		function getShortSignature(signature) {
			if (!/^\S+ \S+\.\S+\(.*\)$/.test(signature)) {
				// this is no method signature, its probably a SQL statement
				return signature;
			}
			var split = signature.substring(0, signature.indexOf('(')).split(".");
			return split[split.length - 2] + '.' + split[split.length - 1];
		}
	});
}
