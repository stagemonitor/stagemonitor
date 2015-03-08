function renderAlertsTab() {

	var metricCategories = {
		TIMER: {
			label: "Timer",
			value: "timers",
			metrics: ["count", "mean", "min", "max", "stddev", "p50", "p75", "p95", "p98", "p99", "p999",
				"mean_rate", "m1_rate", "m5_rate", "m15_rate"]
		},
		GAUGE: {
			label: "Gauge",
			value: "gauges",
			metrics: ["value"]
		},
		METER: {
			label: "Meter",
			value: "meters",
			metrics: ["count", "mean_rate", "m1_rate", "m5_rate", "m15_rate"]
		},
		HISTOGRAM: {
			label: "Histogram",
			value: "histograms",
			metrics: ["count", "mean", "min", "max", "stddev", "p50", "p75", "p95", "p98", "p99", "p999"]
		},
		COUNTER: {
			label: "Counter",
			value: "counters",
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

	function getConfigValue(configurationCategory, configurationKey) {
		return $.grep(stagemonitor.configurationOptions[configurationCategory], function (configOption) {
			return configOption.key == configurationKey;
		})[0].valueAsString;
	}

	function incidentsPage(subscriptionModalTemplate, subscriptionsPartial) {
		var subscriptionsById = JSON.parse(getConfigValue("Alerting", "stagemonitor.alerts.subscriptions"));
		renderSubscriptionsPartial();

		var incidentsTable = $("#incidents-table").dataTable({
			aaSorting: [],
			columns: [
				{ data: "checkName" },
				{
					render: function (data, type, full, meta) {
						return full.hosts.join(', ');
					}
				},
				{
					render: function (data, type, full, meta) {
						return full.instances.join(', ');
					}
				},
				{
					render: function (data, type, full, meta) {
						return new Date(full.firstFailureAt).toISOString();
					}
				},
				{
					render: function (data, type, full, meta) {
						var labelType = "label-success";
						if (full.newStatus == 'WARN') {
							labelType = "label-info"
						} else if (full.newStatus == 'ERROR') {
							labelType = "label-warning"
						} else if (full.newStatus == 'CRITICAL') {
							labelType = "label-danger"
						}
						return '<span class="label ' + labelType + '">' + full.newStatus + '</span>';
					}
				},
				{
					render: function (data, type, full, meta) {
						return '<button class="btn btn-default incident-details-btn">Details</button>';
					}
				}
			]
		});

		$('#incidents-table tbody').on('click', '.incident-details-btn', function () {
			var data = incidentsTable.DataTable().row( $(this).parents('tr') ).data();
			alert(JSON.stringify(data.checkResults, null, 2));
		} );

		$("#alerts-tab").find("a").one('click', function() {
			(function refreshIncidents() {
				// TODO 'push' incidents via the long polling mechanism that is used for ajax request traces
				$.getJSON(stagemonitor.baseUrl + "/stagemonitor/incidents", function (data) {
					updateTable(incidentsTable, data);
					setTimeout(refreshIncidents, 2000);
				});
			}());
		});

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
				alerterTypes: stagemonitor.alerterTypes
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
				data: getChecksArray(),
				aaSorting: [],
				columns: [
					{ data: "application" },
					{ data: "name" },
					{ data: "target" },
					{ data: "alertAfterXFailures" },
					{
						render: function (data, type, full, meta) {
							return '<span class="delete-check glyphicon glyphicon-' + (full.active ? 'ok' : 'remove') + '" aria-hidden="true"></span>';
						}
					},
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
					updateTable(checksTable, getChecksArray());
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
						updateTable(checksTable, getChecksArray());
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
			renderCheckModal("Add Check", {application: stagemonitor.measurementSession.applicationName, active: true, alertAfterXFailures: 1, thresholds: {WARN: [], ERROR: [], CRITICAL: []}});
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
			var metricCategory = metricCategories[check.metricCategory || 'TIMER'];
			$("#check-modal-content").html(checkModalTemplate({
				title: title,
				check: check,
				metricCategories: metricCategories,
				metrics: metricCategory.metrics
			}));
			$(".tip").tooltip({html: true});
			$.getJSON(stagemonitor.baseUrl + "/stagemonitor/metrics", function (metrics) {
				var source = Object.keys(metrics[metricCategory.value]);
				updateMatchesCount();
				$("#target-input").typeahead({
						hint: true,
						highlight: true,
						minLength: 0
					},
					{
						name: 'targets',
						displayKey: 'value',
						source: substringMatcher($.map(source, function(str) { return RegExp.quote(str) }))
					}).on('keyup change', updateMatchesCount);

				function updateMatchesCount() {
					var matches = 0;
					var regExp = new RegExp($("#target-input").val());
					for (var i = 0; i < source.length; i++) {
						if (regExp.test(source[i])) {
							matches++;
						}
					}
					$("#target-matches-input").val(matches);
				}
			});
		}

		var substringMatcher = function(strs) {
			return function findMatches(q, cb) {
				var matches, substrRegex;

				// an array that will be populated with substring matches
				matches = [];

				// regex used to determine if a string contains the substring `q`
				substrRegex = new RegExp(q, 'i');

				// iterate through the pool of strings and for any string that
				// contains the substring `q`, add it to the `matches` array
				$.each(strs, function(i, str) {
					if (substrRegex.test(str)) {
						// the typeahead jQuery plugin expects suggestions to a
						// JavaScript object, refer to typeahead docs for more info
						matches.push({ value: str });
					}
				});

				cb(matches);
			};
		};

		function getChecksArray() {
			var data = $.map(checksById, function (value, index) {
				return [value];
			});
			return data;
		}
	}

	function updateTable(table, data) {
		table.fnClearTable(false);
		if (data.length > 0) {
			table.fnAddData(data, false);
		}
		table.DataTable().draw(false);
	}
}