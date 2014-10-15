renderConfigTab = function(configurationSources, configurationOptions, passwordSet, contextPathPrefix) {
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
				$button.nextAll(".submit-response-ok").show().fadeOut(3000);
			}).fail(function (xhr) {
				$button.removeClass("btn-primary").addClass("btn-danger");
				var errorSpan = $button.nextAll(".submit-response-failed");
				errorSpan.html(xhr.responseText || 'Failed to save.');
				errorSpan.show().fadeOut(4000, function () {
					$button.removeClass("btn-danger").addClass("btn-primary");
				});
			});
		return false;
	});
};
