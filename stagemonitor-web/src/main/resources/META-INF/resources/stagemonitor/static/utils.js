var utils = (function() {
	RegExp.quote = function(str) {
		return (str+'').replace(/([.?*+^$[\]\\(){}|-])/g, "\\$1");
	};
	return {
		loadScripts: function(scripts, callback) {
			$.when.apply(null, $.map(scripts, loadScript)).done(function () {
				callback();
			});
			function loadScript(path) {
				var result = $.Deferred(),
					script = document.createElement("script");
				script.async = "async";
				script.type = "text/javascript";
				script.src = path;
				script.onload = script.onreadystatechange = function (_, isAbort) {
					if (!script.readyState || /loaded|complete/.test(script.readyState)) {
						if (isAbort)
							result.reject();
						else
							result.resolve();
					}
				};
				script.onerror = function () { result.reject(); };
				$("head")[0].appendChild(script);
				return result.promise();
			}
		},
		clone: function(object) {
			return JSON.parse(JSON.stringify(object));
		}
	}
})();