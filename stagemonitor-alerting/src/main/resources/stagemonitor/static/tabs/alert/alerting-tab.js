(function (){
	tabPlugins.push({
		label: "Alerts",
		tabId: "alerts-tab",
		tabContentId: "stagemonitor-alerts",
		tooltip: "Show status and current incidents of the application.",
		renderTab: function(tmpl) {

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
								return createStatusLabel(full.newStatus);
							}
						},
						{ data: "failedChecks"},
						{
							render: function (data, type, full, meta) {
								return '<a href="#" class="btn btn-default incident-details-btn" ' +
									'data-toggle="modal" data-target="#incident-details-modal">Details</a>';
							}
						}
					]
				});

				var incidentDetailsTable = $("#incident-details-table").dataTable({
					order: [[ 0, 'asc' ], [ 1, 'asc' ], [ 2, 'asc' ], [ 3, 'asc' ]],
					columns: [
						{ data: "application" },
						{ data: "host" },
						{ data: "instance" },
						{
							render: function (data, type, full, meta) {
								return createStatusLabel(full.status);
							}
						},
						{ data: "failingExpression" },
						{ data: "currentValue" }
					]
				});


				function createStatusLabel(status) {
					var labelType = "label-success";
					if (status == 'WARN') {
						labelType = "label-warning"
					} else if (status == 'CRITICAL' || status == 'ERROR') {
						labelType = "label-danger"
					}
					return '<span class="label ' + labelType + '">' + status + '</span>';
				}

				$('#incidents-table tbody').on('click', '.incident-details-btn', function () {
					var incident = incidentsTable.DataTable().row( $(this).parents('tr') ).data();
					var data = [];
					$.each(incident.checkResults, function (i, results) {
						$.each(results.results, function (i, result) {
							data.push({
								application: results.measurementSession.applicationName,
								host: results.measurementSession.hostName,
								instance: results.measurementSession.instanceName,
								status: result.status,
								failingExpression: result.failingExpression,
								currentValue: +(result.currentValue).toFixed(2)
							});
						});
					});
					updateTable(incidentDetailsTable, data);
				} );

				$("#alerts-tab").find("a").one('click', function() {
					(function refreshIncidents() {
						$.getJSON(stagemonitor.baseUrl + "/stagemonitor/incidents", function (data) {
							updateTable(incidentsTable, data.incidents);
							$("#incident-status").html(createStatusLabel(data.status));
							setTimeout(refreshIncidents, 2000);
						});
					}());
				});

				$("#subscriptions").on("click", "#add-subscription", function () {
					renderSubscriptionModal("Add Subscription", {
						alertOnBackToOk: true, alertOnWarn: true, alertOnError: true, alertOnCritical: true
					});
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
							utils.errorMessage("Failed to remove subscription", xhr);
						});
				});

				function renderSubscriptionsPartial() {
					$("#subscriptions-partial").html(subscriptionsPartial({subscriptionsById: subscriptionsById}))
				}

				var $subscriptionModal = $("#subscription-modal");
				$subscriptionModal.on('click', "#test-alert", function () {
					var $subscriptionForm = $("#subscription-form");
					if ($subscriptionForm.parsley().validate()) {
						var subscription = $subscriptionForm.serializeObject();

						$.ajax({
							url: stagemonitor.baseUrl + "/stagemonitor/test-alert?status=WARN",
							type: 'POST',
							data: JSON.stringify(subscription),
							contentType: 'application/json'
						}).done(function () {
								utils.successMessage("Successfully sent test alert");
							}).fail(function (xhr) {
								utils.errorMessage("Failed to send test alert", xhr);
							});
					}
				});

				$subscriptionModal.on('click', "#save-subscription", function () {
					var $subscriptionForm = $("#subscription-form");
					if ($subscriptionForm.parsley().validate()) {
						$subscriptionModal.modal('hide');
						var subscription = $subscriptionForm.serializeObject();
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
								utils.errorMessage("Failed to save subscription", xhr);
							});
					}
				});

				$subscriptionModal.on('change', "#alerter-type-select", function () {
					var optionSelected = $("option:selected", this);
					$("#alerter-target").html(optionSelected.data('target-label'));
				});

				function renderSubscriptionModal(title, subscription) {
					$.getJSON(stagemonitor.baseUrl + "/stagemonitor/alerter-types", function(alerterTypes) {
						$("#subscription-modal-content").html(subscriptionModalTemplate({
							title: title,
							subscription: subscription,
							alerterTypes: alerterTypes
						}));
						// trigger onChange
						var activeAlerterType = subscription.alerterType || alerterTypes[0].alerterType;
						$("#alerter-type-select").val(activeAlerterType).change();

						$(".tip").tooltip({html: true});
					});
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
							{
								render: function (data, type, full, meta) {
									return utils.metricAsString(full.target, "");
								}
							},
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
							utils.errorMessage("Failed to remove check", xhr);
						});
				});

				var $checkModal = $("#check-modal");
				$checkModal.on('click', "#save-check", function () {
					if ($("#check-form").parsley().validate()) {
						$checkModal.modal('hide');
						var check = getCheckFromForm();
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
								utils.errorMessage("Failed to save check", xhr);
							});
					}
				});

				function rerenderCheckModal() {
					renderCheckModal($("#check-modal-label").html(), getCheckFromForm());
				}
				$checkModal.on('change', "#target-name-input, .tag-key, .tag-value, .tt-dropdown-menu", function () {
					rerenderCheckModal();
				});
				$checkModal.on('typeahead:change', ".typeahead", function () {
					rerenderCheckModal();
				});

				$checkModal.on('click', ".remove-threshold", function () {
					$(this).parents(".threshold-form-group").remove();
					rerenderCheckModal();
				});

				$checkModal.on('click', ".add-threshold", function () {
					var check = getCheckFromForm();
					check.thresholds[$(this).data('severity')].push({});
					renderCheckModal($("#check-modal-label").html(), check);
				});

				$checkModal.on('click', ".add-tag-filter", function () {
					var check = getCheckFromForm();
					check.target.tags[""] = "";
					renderCheckModal($("#check-modal-label").html(), check);
				});

				$checkModal.on('click', ".add-tag-filter", function () {
					var check = getCheckFromForm();
					check.target.tags = check.target.tags || {};
					check.target.tags[""] = "";
					renderCheckModal($("#check-modal-label").html(), check);
				});

				$checkModal.on('click', ".remove-tag-filter", function () {
					var check = getCheckFromForm();
					delete check.target.tags[$(this).data('tag')];
					renderCheckModal($("#check-modal-label").html(), check);
				});

				$("#checks-table").on("click", ".edit-check", function () {
					renderCheckModal("Edit Check", checksById[$(this).data('check-id')]);
				});

				$(".add-check").click(function () {
					renderCheckModal("Add Check", {
						application: stagemonitor.measurementSession.applicationName,
						active: true,
						alertAfterXFailures: 1,
						thresholds: {WARN: [], ERROR: [], CRITICAL: []},
						target: {
							tags: {}
						}
					});
				});

				function getCheckFromForm() {
					var check = $("#check-form").serializeObject();

					check.target = check.target || {};
					check.target.tags = check.target.tags || {};
					var tagKeys = check.target.tagKeys || [];
					var tagValues = check.target.tagValues || [];
					for (var i = 0; i < tagKeys.length; i++) {
						check.target.tags[tagKeys[i]] = tagValues[i];
					}
					delete check.target.tagKeys;
					delete check.target.tagValues;

					check.thresholds = check.thresholds || {};
					check.thresholds.WARN = check.thresholds.WARN || [];
					check.thresholds.ERROR = check.thresholds.ERROR || [];
					check.thresholds.CRITICAL = check.thresholds.CRITICAL || [];
					return check;
				}

				function renderCheckModal(title, check) {
					$(".tip").tooltip({html: true});
					$.getJSON(stagemonitor.baseUrl + "/stagemonitor/metrics", function (metrics) {

						var matchingMetrics = getMatchingMetrics(metrics, check.target);
						var matchingValueTypes = $.unique([].concat.apply([], $.map(matchingMetrics, function(metric) { return Object.keys(metric.values) })));
						$("#check-modal-content").html(checkModalTemplate({
							title: title,
							check: check,
							hasTagFilters: Object.keys(check.target.tags).length > 0,
							valueTypes: matchingValueTypes
						}));
						var allMetricNames = $.unique($.map(metrics, function(metric) { return metric.name }));

						typeahead("#target-name-input", allMetricNames);
						var matchingTagKeys = $.unique([].concat.apply([], $.map(matchingMetrics, function(metric) { return Object.keys(metric.tags) })));
						typeahead(".tag-key", matchingTagKeys);

						var tagKeys = Object.keys(check.target.tags);
						for (var i = 0; i < tagKeys.length; i++) {
							var tagKey = tagKeys[i];
							if (tagKey) {
								var matchingTagValues = $.unique($.map(matchingMetrics, function (metric) { return metric.tags[tagKey] }));
								typeahead("#tag-value-" + tagKey, matchingTagValues);
							}
						}

						$("#target-matches-input").val(matchingMetrics.length);
					});
				}

				function typeahead(selector, source) {
					$(selector).typeahead({
							hint: true,
							highlight: true,
							minLength: 0
						},
						{
							name: 'targets',
							displayKey: 'value',
							source: substringMatcher(source),
							limit: 100
						});
				}

				function getMatchingMetrics(metrics, target) {
					var matchingMetrics = [];
					for (var i = 0; i < metrics.length; i++) {
						var metric = metrics[i];
						if (utils.matches(metric, target)) {
							matchingMetrics.push(metric)
						}
					}
					return matchingMetrics;
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
	});
})();