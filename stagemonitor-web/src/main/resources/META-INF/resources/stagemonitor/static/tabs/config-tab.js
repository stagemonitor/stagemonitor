renderConfigTab = function (configurationSources, configurationOptions, passwordSet) {
	$.get("tabs/config-tab.html", function (template) {
		var configurationTemplate = Handlebars.compile($(template).html());

		var $configTab = $("#stagemonitor-configuration");
		$configTab.html(configurationTemplate({
			configurationOptions: configurationOptions,
			configurationSources: configurationSources,
			passwordSet: passwordSet
		}));
		$configTab.on("click", ".save-configuration", function () {
			var $button = $(this);
			$.post(stagemonitor.baseUrl + "/stagemonitor/configuration", $(this.form).add("#password-form").serialize())
				.done(function () {
					utils.successMessage($button.data("success"));
				}).fail(function (xhr) {
					utils.errorMessage($button.data("fail"), xhr);
				});
			return false;
		});
	});
};
