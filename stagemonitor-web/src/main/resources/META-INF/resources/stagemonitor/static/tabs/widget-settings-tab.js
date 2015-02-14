$(document).ready(function () {
	$("#widget-settings-save").on("click", function () {
		$("input[data-widget-settings-key]").each(function () {
			var key = $(this).attr("data-widget-settings-key");
			if ($(this).attr("type") === "checkbox") {
				var value = $(this).prop("checked");
				localStorage.setItem(key, value);
			} else {
				var value = $(this).val();
				localStorage.setItem(key, value);
			}
		});
		$.growl('Changes will take effect on next request', {
			type: "success"
		});
		return false;
	});

	$("input[data-widget-settings-key]").each(function () {
		var key = $(this).attr("data-widget-settings-key");
		var value = localStorage.getItem(key);
		if (value == null) {
			var defaultValue = $(this).attr("data-widget-settings-default-value");
			value = defaultValue;
			localStorage.setItem(key, defaultValue);
		}

		if ($(this).attr("type") == "checkbox") {
			$(this).prop("checked", value == "true");
		} else {
			$(this).val(value);
		}
	});

	// spinner
	$('.stagemonitor-spinner .btn:first-of-type').on('click', function () {
		var $input = $(this).parent().prev();
		var max = $input.data('max') || Infinity;
		$input.val(Math.min(parseInt($input.val(), 10) + 1, max));
		return false;
	});
	$('.stagemonitor-spinner .btn:last-of-type').on('click', function () {
		var $input = $(this).parent().prev();
		var min = $input.data('min') || -Infinity;
		$input.val(Math.max(parseInt($input.val(), 10) - 1, min));
		return false;
	});

});
