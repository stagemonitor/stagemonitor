renderConfigTab = function (configurationSources, configurationOptions, passwordSet, contextPathPrefix) {
	var configurationTemplate = Handlebars.compile($("#stagemonitor-configuration-template").html());

	var $configTab = $("#stagemonitor-configuration");
	$configTab.html(configurationTemplate({
		configurationOptions: configurationOptions,
		configurationSources: configurationSources,
		passwordSet: passwordSet
	}));
	$configTab.on("click", ".save-configuration", function () {
		var $button = $(this);
		$.post(contextPathPrefix + "/stagemonitor/configuration", $(this.form).add("#password-form").serialize())
			.done(function () {
				$.growl($button.data("success"), { type: "success" });
			}).fail(function (xhr) {
				$.growl((htmlEscape(xhr.responseText) || $button.data("fail")), { type: "danger" });
			});
		return false;
	});

	function htmlEscape(str) {
		return String(str)
			.replace(/&/g, '&amp;')
			.replace(/"/g, '&quot;')
			.replace(/'/g, '&#39;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;');
	}
};
