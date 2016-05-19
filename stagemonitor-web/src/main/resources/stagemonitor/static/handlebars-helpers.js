Handlebars.registerHelper('ifCond', function (v1, operator, v2, options) {
	switch (operator) {
		case '==':
			return (v1 == v2) ? options.fn(this) : options.inverse(this);
		case '===':
			return (v1 === v2) ? options.fn(this) : options.inverse(this);
		case '<':
			return (v1 < v2) ? options.fn(this) : options.inverse(this);
		case '<=':
			return (v1 <= v2) ? options.fn(this) : options.inverse(this);
		case '>':
			return (v1 > v2) ? options.fn(this) : options.inverse(this);
		case '>=':
			return (v1 >= v2) ? options.fn(this) : options.inverse(this);
		case '&&':
			return (v1 && v2) ? options.fn(this) : options.inverse(this);
		case '||':
			return (v1 || v2) ? options.fn(this) : options.inverse(this);
		default:
			return options.inverse(this);
	}
});
Handlebars.registerHelper('csv', function (items, options) {
	return options.fn(items.join(', '));
});
Handlebars.registerHelper('capitalize', function (string, options) {
	return string.charAt(0).toUpperCase() + string.slice(1).toLowerCase();
});
Handlebars.registerHelper('wrapWithinPreIfIoQuery', function(ioQuery, shortSignature, signature) {
	if (ioQuery) {
		return new Handlebars.SafeString("<pre>" + Handlebars.escapeExpression(signature) + "</pre>");
	} else {
		return new Handlebars.SafeString(Handlebars.escapeExpression(shortSignature ? shortSignature : signature));
	}
});
Handlebars.registerHelper('breaklines', function(text) {
	text = Handlebars.Utils.escapeExpression(text);
	text = text.replace(/(\r\n|\n|\r)/gm, '<br>');
	return new Handlebars.SafeString(text);
});
