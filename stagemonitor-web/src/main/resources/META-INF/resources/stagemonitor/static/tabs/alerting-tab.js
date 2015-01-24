function renderAlertsTab() {
	$.get("tabs/alerting-tab.html", function (tmpl) {
		var template = Handlebars.compile($(tmpl).html());
		var renderedTemplate = template();
		var tab = $("#stagemonitor-alerts");
		tab.html(renderedTemplate);

		initSideMenu();
		$("#incidents-table").dataTable({
			"bJQueryUI": true,
			"sPaginationType": "full_numbers"
		});
		$("#checks-table").dataTable({
			"bJQueryUI": true,
			"sPaginationType": "full_numbers"
		});
	});


	function initSideMenu() {
		// on selection from the side bar
		$("#alerting-side-menu").find("a").click(function () {
			var thisLink = $(this);
			$(".alerting-menu").removeClass("active");
			thisLink.parent().addClass("active");
			$(".alerting-content").addClass("hidden");
			var pluginId = thisLink.attr("href");
			$(pluginId).removeClass("hidden");
			return false;
		});
	}
}