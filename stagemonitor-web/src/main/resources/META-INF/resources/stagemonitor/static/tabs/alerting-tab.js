function renderAlertsTab() {

	var metricCategories = {
		TIMER: {
			label: "Timer",
			metrics: ["count", "mean", "min", "max", "stddev", "p50", "p75", "p95", "p98", "p99", "p999",
				"mean_rate", "m1_rate", "m5_rate", "m15_rate"]
		},
		GAUGE: {
			label: "Gauge",
			metrics: ["value"]
		},
		METER: {
			label: "Meter",
			metrics: ["count", "mean_rate", "m1_rate", "m5_rate", "m15_rate"]
		},
		HISTOGRAM: {
			label: "Histogram",
			metrics: ["count", "mean", "min", "max", "stddev", "p50", "p75", "p95", "p98", "p99", "p999"]
		},
		COUNTER: {
			label: "Counter",
			metrics: ["count"]
		}
	};


	$.get("tabs/alerting-tab.hbs", function (tmpl) {
		var $tmpl = $(tmpl);
		var template = Handlebars.compile($tmpl.find("#alerts-content-template").html());
		var checkModalTemplate = Handlebars.compile($tmpl.find("#check-modal-template").html());
		var subscriptionModalTemplate = Handlebars.compile($tmpl.find("#subscription-modal-template").html());
		var subscriptionsPartial = Handlebars.compile($tmpl.find("#subscriptions-partial-template").html());
		var tab = $("#stagemonitor-alerts");
		tab.html(template());

		initSideMenu();

		initializeValidation();
		incidentsPage(subscriptionModalTemplate, subscriptionsPartial);
		checksPage(checkModalTemplate);
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

	function initializeValidation() {
		window.ParsleyConfig = {
			errorClass: 'has-error',
			successClass: 'has-success',
			classHandler: function (ParsleyField) {
				return ParsleyField.$element.parents('.form-group');
			},
			errorsWrapper: '<span class="help-block">',
			errorTemplate: '<div></div>'
		};
	}

	function getSubscriptionsUrl(id) {
		return stagemonitor.baseUrl + "/stagemonitor/subscriptions?" + $("#password-form").serialize() + "&id=" + id;
	}

	function getConfigValue(configurationCategory, configurationKey) {
		return $.grep(stagemonitor.configurationOptions[configurationCategory], function (configOption) {
			return configOption.key == configurationKey;
		})[0].valueAsString;
	}

	function incidentsPage(subscriptionModalTemplate, subscriptionsPartial) {
		var incidents = [];
		var alerterTypes = ["Email", "PagerDuty", "SMS"];
		var subscriptionsById = JSON.parse(getConfigValue("Alerting", "stagemonitor.alerts.subscriptions"));
		renderSubscriptionsPartial();

		$("#incidents-table").dataTable();

		$("#subscriptions").on("click", "#add-subscription", function () {
			renderSubscriptionModal("Add Subscription", {});
		});

		$("#subscriptions").on("click", ".edit-subscription", function () {
			renderSubscriptionModal("Edit Subscription", subscriptionsById[$(this).parent().parent().data('subscription-id')]);
		});

		$("#subscriptions").on("click", ".remove-subscription", function () {
			var id = $(this).parent().parent().data('subscription-id');

			var clonedSubscriptions = utils.clone(subscriptionsById);
			delete clonedSubscriptions[id];
			$.post(stagemonitor.baseUrl + "/stagemonitor/configuration",
					getConfigData("stagemonitor.alerts.subscriptions", clonedSubscriptions))
				.done(function () {
					utils.successMessage("Successfully removed subscription");
					subscriptionsById = clonedSubscriptions;
					renderSubscriptionsPartial();
				}).fail(function (xhr) {
					utils.errorMessage(xhr.responseText || "Failed to remove subscription");
				});
		});

		function renderSubscriptionsPartial() {
			$("#subscriptions-partial").html(subscriptionsPartial({subscriptionsById: subscriptionsById}))
		}

		var $subscriptionModal = $("#subscription-modal");
		$subscriptionModal.on('click', "#save-subscription", function () {
			var $subscriptionForm = $("#subscription-form");
			if ($subscriptionForm.parsley().validate()) {
				$subscriptionModal.modal('hide');
				var subscription = $subscriptionForm.serializeObject();
				console.log(JSON.stringify(subscription));
				if (!subscription.id) {
					subscription.id = utils.generateUUID();
				}

				var clonedSubscriptions = utils.clone(subscriptionsById);
				clonedSubscriptions[subscription.id] = subscription;
				$.post(stagemonitor.baseUrl + "/stagemonitor/configuration",
						getConfigData("stagemonitor.alerts.subscriptions", clonedSubscriptions)).done(function () {
						utils.successMessage("Successfully saved subscription into " + $("#configuration-source").val());
						subscriptionsById = clonedSubscriptions;
						renderSubscriptionsPartial();
					}).fail(function (xhr) {
						utils.errorMessage(xhr.responseText || "Failed to save subscription");
					});
			}
		});

		function renderSubscriptionModal(title, subscription) {
			$("#subscription-modal-content").html(subscriptionModalTemplate({
				title: title,
				subscription: subscription,
				alerterTypes: alerterTypes
			}));
		}
	}

	function getConfigData(key, value) {
		return {
			key: key,
			value: JSON.stringify(value),
			configurationSource: $("#configuration-source").val(),
			"stagemonitor.password": $("#stagemonitor-password").val()
		};
	}

	function checksPage(checkModalTemplate) {
		var checksById = JSON.parse(getConfigValue("Alerting", "stagemonitor.alerts.checks"));
		var checksTable;

		renderChecksTable();

		function renderChecksTable() {
			checksTable = $("#checks-table").dataTable({
				data: getData(),
				aaSorting: [],
				columns: [
					{ data: "name" },
					{ data: "target" },
					{ data: "alertAfterXFailures" },
					{
						render: function (data, type, full, meta) {
							return '<a href="#"><span class="edit-check glyphicon glyphicon-edit" aria-hidden="true" ' +
								'data-toggle="modal" data-target="#check-modal" data-check-id="' + full.id + '"></span></a>';
						}
					},
					{
						render: function (data, type, full, meta) {
							return '<a href="#"><span class="delete-check glyphicon glyphicon-remove" aria-hidden="true" ' +
								'data-check-id="' + full.id + '"></span></a>'
						}
					}
				]
			});
		}

		$("#checks-table").on('click', '.delete-check', function () {
			var id = $(this).data('check-id');
			var clonedChecks = utils.clone(checksById);
			delete clonedChecks[id];

			$.post(stagemonitor.baseUrl + "/stagemonitor/configuration",
					getConfigData("stagemonitor.alerts.checks", clonedChecks))
				.done(function () {
					utils.successMessage("Successfully removed check");
					checksById = clonedChecks;
					updateChecksTable();
				}).fail(function (xhr) {
					utils.errorMessage(xhr.responseText || "Failed to remove check");
				});
		});

		var $checkModal = $("#check-modal");
		$checkModal.on('click', "#save-check", function () {
			if ($("#check-form").parsley().validate()) {
				$checkModal.modal('hide');
				var check = getCheckFromForm();
				console.log(JSON.stringify(check));
				if (!check.id) {
					check.id = utils.generateUUID();
				}

				var clonedChecks = utils.clone(checksById);
				clonedChecks[check.id] = check;
				$.post(stagemonitor.baseUrl + "/stagemonitor/configuration",
						getConfigData("stagemonitor.alerts.checks", clonedChecks))
					.done(function () {
						utils.successMessage("Successfully saved check into " + $("#configuration-source").val());
						checksById = clonedChecks;
						updateChecksTable();
					}).fail(function (xhr) {
						utils.errorMessage(xhr.responseText || "Failed to save check");
					});
			}
		});


		$checkModal.on('change', "#metric-category-input", function () {
			renderCheckModal($("#check-modal-label").html(), getCheckFromForm());
		});

		$checkModal.on('click', ".remove-threshold", function () {
			$(this).parents(".threshold-form-group").remove();
			renderCheckModal($("#check-modal-label").html(), getCheckFromForm());
		});

		$checkModal.on('click', ".add-threshold", function () {
			var check = getCheckFromForm();
			check.thresholds[$(this).data('severity')].push({});
			renderCheckModal($("#check-modal-label").html(), check);
		});

		$("#checks-table").on("click", ".edit-check", function () {
			renderCheckModal("Edit Check", checksById[$(this).data('check-id')]);
		});

		$(".add-check").click(function () {
			renderCheckModal("Add Check", {alertAfterXFailures: 1, thresholds: {WARN: [], ERROR: [], CRITICAL: []}});
		});


		function getCheckFromForm() {
			var check = $("#check-form").serializeObject();
			check.thresholds = check.thresholds || {};
			check.thresholds.WARN = check.thresholds.WARN || [];
			check.thresholds.ERROR = check.thresholds.ERROR || [];
			check.thresholds.CRITICAL = check.thresholds.CRITICAL || [];
			return check;
		}

		function renderCheckModal(title, check) {
			$("#check-modal-content").html(checkModalTemplate({
				title: title,
				check: check,
				metricCategories: metricCategories,
				metrics: metricCategories[check.metricCategory || 'TIMER'].metrics
			}));
		}

		function updateChecksTable() {
			checksTable.fnClearTable(false);
			var data = getData();
			if (data.length > 0) {
				checksTable.fnAddData(data, false);
			}
			checksTable.DataTable().draw(false);
		}

		function getData() {
			var data = $.map(checksById, function (value, index) {
				return [value];
			});
			return data;
		}

		function getChecksUrl(id) {
			return stagemonitor.baseUrl + "/stagemonitor/checks?" + $("#password-form").serialize() + "&id=" + id;
		}
	}
}