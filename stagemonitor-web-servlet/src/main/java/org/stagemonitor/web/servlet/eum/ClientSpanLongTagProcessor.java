package org.stagemonitor.web.servlet.eum;

public class ClientSpanLongTagProcessor extends AbstractNumberClientSpanTagProcessor<Long> {

	public ClientSpanLongTagProcessor(String type, String tagName, String requestParameterName) {
		super(type, tagName, requestParameterName, Long.MIN_VALUE, Long.MAX_VALUE);
	}

	@Override
	protected boolean inBounds(Long value)  {
		return lowerBound <= value && value <= upperBound;
	}

	@Override
	protected Long parse(String valueOrNull) {
		return Long.parseLong(valueOrNull);
	}
}
