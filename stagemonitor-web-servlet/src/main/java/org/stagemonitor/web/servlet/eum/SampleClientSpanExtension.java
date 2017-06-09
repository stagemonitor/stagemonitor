package org.stagemonitor.web.servlet.eum;

import java.util.List;

import static java.util.Collections.singletonList;

public class SampleClientSpanExtension extends ClientSpanExtensionSPI {
	@Override
	public String getClientTraceExtensionScript() {
		return "var eum = window.EumObject; (window[eum])('meta', 'sample_span_extension', 'sample_value');";
	}

	@Override
	public List<String> getWhitelistedTags() {
		return singletonList("sample_span_extension");
	}
}
