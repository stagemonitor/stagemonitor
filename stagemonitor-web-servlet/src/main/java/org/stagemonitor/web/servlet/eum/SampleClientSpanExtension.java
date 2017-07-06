package org.stagemonitor.web.servlet.eum;

import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.stagemonitor.web.servlet.eum.ClientSpanMetadataTagProcessor.TYPE_STRING;

public class SampleClientSpanExtension extends ClientSpanExtension {
	@Override
	public String getClientTraceExtensionScript() {
		return "var eum = window.EumObject; (window[eum])('meta', 'sample_span_extension', 'sample_value');";
	}

	@Override
	public Map<String, String> getWhitelistedTags() {
		return singletonMap("sample_span_extension", TYPE_STRING);
	}
}
