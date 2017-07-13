package org.stagemonitor.web.servlet.eum;

import java.util.Collections;
import java.util.Map;

import io.opentracing.Span;

class ClientSpanLongTagProcessor extends ClientSpanTagProcessor {

	private final String tagName;
	private final String requestParameterName;
	private long lowerBound = Long.MIN_VALUE;
	private long upperBound = Long.MAX_VALUE;
	private boolean discardSpanOnBoundViolation = false;

	ClientSpanLongTagProcessor(String type, String tagName, String requestParameterName) {
		super(type, Collections.singletonList(requestParameterName));
		this.tagName = tagName;
		this.requestParameterName = requestParameterName;
	}

	@Override
	protected void processSpanImpl(Span span, Map<String, String[]> servletRequestParameters) {
		final String valueOrNull = getParameterValueOrNull(requestParameterName, servletRequestParameters);
		final Long parsedLongOrNull = parsedLongOrNull(valueOrNull);
		if (parsedLongOrNull != null) {
			if (inBounds(parsedLongOrNull)) {
				span.setTag(tagName, parsedLongOrNull);
			} else if (discardSpanOnBoundViolation) {
				discardSpan(span);
			}
		}
	}

	private boolean inBounds(Long longValue) {
		return lowerBound <= longValue && longValue <= upperBound;
	}

	public ClientSpanLongTagProcessor lowerBound(long lowerBound) {
		this.lowerBound = lowerBound;
		return this;
	}

	public ClientSpanLongTagProcessor upperBound(long upperBound) {
		this.upperBound = upperBound;
		return this;
	}

	public ClientSpanLongTagProcessor discardSpanOnBoundViolation(boolean discardSpanOnBoundViolation) {
		this.discardSpanOnBoundViolation = discardSpanOnBoundViolation;
		return this;
	}

}
