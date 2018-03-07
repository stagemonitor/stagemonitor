package org.stagemonitor.web.servlet.eum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

import io.opentracing.Span;

public abstract class AbstractNumberClientSpanTagProcessor<N extends Number> extends ClientSpanTagProcessor {

	private static final Logger logger = LoggerFactory.getLogger(AbstractNumberClientSpanTagProcessor.class);
	private final String tagName;
	private final String requestParameterName;
	protected N lowerBound;
	protected N upperBound;
	private boolean discardSpanOnBoundViolation = false;

	public AbstractNumberClientSpanTagProcessor(String type, String tagName, String requestParameterName, N lowerBound, N upperBound) {
		super(type, Collections.singletonList(requestParameterName));
		this.tagName = tagName;
		this.requestParameterName = requestParameterName;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	@Override
	protected void processSpanImpl(Span span, Map<String, String[]> servletRequestParameters) {
		final String valueOrNull = getParameterValueOrNull(requestParameterName, servletRequestParameters);
		final N parsedNumberOrNull = parsedNumberOrNull(valueOrNull);
		if (parsedNumberOrNull != null) {
			if (inBounds(parsedNumberOrNull)) {
				span.setTag(tagName, parsedNumberOrNull);
			} else if (discardSpanOnBoundViolation) {
				logger.info("discarding {} for illegal value '{}': not in bounds ({} <= {} <= {})",
						tagName, valueOrNull, lowerBound, valueOrNull, upperBound);
				discardSpan(span);
			}
		}
	}

	protected abstract boolean inBounds(N value);

	public AbstractNumberClientSpanTagProcessor<N> lowerBound(N lowerBound) {
		this.lowerBound = lowerBound;
		return this;
	}

	public AbstractNumberClientSpanTagProcessor<N> upperBound(N upperBound) {
		this.upperBound = upperBound;
		return this;
	}

	public AbstractNumberClientSpanTagProcessor<N> discardSpanOnBoundViolation(boolean discardSpanOnBoundViolation) {
		this.discardSpanOnBoundViolation = discardSpanOnBoundViolation;
		return this;
	}

	protected N parsedNumberOrNull(String valueOrNull) {
		try {
			return parse(valueOrNull);
		} catch (NumberFormatException e) {
			logger.info("discarding {} for illegal value '{}': not a parseable number", tagName, valueOrNull);
			return null;
		}
	}

	protected abstract N parse(String valueOrNull);

}
