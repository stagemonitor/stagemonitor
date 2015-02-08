var utils = (function () {
	RegExp.quote = function (str) {
		return (str + '').replace(/([.?*+^$[\]\\(){}|-])/g, "\\$1");
	};

	function loadScript(path) {
		var result = $.Deferred(),
			script = document.createElement("script");
		script.async = "async";
		script.type = "text/javascript";
		script.src = stagemonitor.contextPath + path;
		script.onload = script.onreadystatechange = function (_, isAbort) {
			if (!script.readyState || /loaded|complete/.test(script.readyState)) {
				if (isAbort)
					result.reject();
				else
					result.resolve();
			}
		};
		script.onerror = function () {
			result.reject();
		};
		$("head")[0].appendChild(script);
		return  result.promise();
	}

	return {
		loadScripts: function (scripts, callback) {
			$.when.apply(null, $.map(scripts, loadScript)).done(function () {
				callback();
			});
		},
		clone: function (object) {
			return JSON.parse(JSON.stringify(object));
		},
		objectToValuesArray: function (obj) {
			var data = [];
			for (var propertyName in obj) {
				data.push(obj[propertyName]);
			}
			return data;
		},
		generateUUID: function () {
			var d = window.performance && window.performance.now && window.performance.now() || new Date().getTime();
			return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
				var r = (d + Math.random() * 16) % 16 | 0;
				d = Math.floor(d / 16);
				return (c == 'x' ? r : (r & 0x7 | 0x8)).toString(16);
			});
		}
	}
})();