package org.stagemonitor.web.servlet.eum;

import java.util.List;
import java.util.Map;

import io.opentracing.Span;

import static java.util.Arrays.asList;
import static org.stagemonitor.web.servlet.eum.ClientSpanServlet.TYPE_PAGE_LOAD;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_APP_CACHE_LOOKUP;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_DNS_LOOKUP;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_REDIRECT;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_REQUEST;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_RESOURCE;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_RESPONSE;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_SSL;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_TCP;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.getWeaselRequestParameterName;

public class ResourceTimingProcessor extends ClientSpanTagProcessor {

	private static final List<String> weaselParametersToSum = asList(
			getWeaselRequestParameterName(TIMING_REDIRECT),
			getWeaselRequestParameterName(TIMING_APP_CACHE_LOOKUP),
			getWeaselRequestParameterName(TIMING_DNS_LOOKUP),
			getWeaselRequestParameterName(TIMING_TCP),
			getWeaselRequestParameterName(TIMING_SSL),
			getWeaselRequestParameterName(TIMING_REQUEST),
			getWeaselRequestParameterName(TIMING_RESPONSE));

	ResourceTimingProcessor() {
		super(TYPE_PAGE_LOAD, weaselParametersToSum);
	}

	@Override
	protected void processSpanImpl(Span span, Map<String, String[]> servletRequestParameters) {
		long sum = 0;
		for (String weaselParameterToSum : weaselParametersToSum) {
			final Long timing = getParameterValueAsLongOrNull(weaselParameterToSum, servletRequestParameters);
			if (timing == null) {
				return;
			} else {
				sum += timing;
			}
		}
		span.setTag(TIMING_RESOURCE, sum);
	}
}
