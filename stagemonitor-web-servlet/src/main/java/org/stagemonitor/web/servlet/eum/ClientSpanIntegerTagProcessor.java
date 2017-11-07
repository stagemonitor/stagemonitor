package org.stagemonitor.web.servlet.eum;

public class ClientSpanIntegerTagProcessor extends AbstractNumberClientSpanTagProcessor<Integer> {

	public ClientSpanIntegerTagProcessor(String type, String tagName, String requestParameterName) {
		super(type, tagName, requestParameterName, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	@Override
	protected boolean inBounds(Integer value)  {
		return lowerBound <= value && value <= upperBound;
	}

	@Override
	protected Integer parse(String valueOrNull) {
		return Integer.parseInt(valueOrNull);
	}
}
