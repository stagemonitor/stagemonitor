package org.stagemonitor.core.util;

import org.junit.Assert;
import org.junit.Test;

public class GraphiteSanitizerTest {

	@Test
	public void testEncodeForGraphite() throws Exception {
		Assert.assertEquals("GET-|index:html", GraphiteSanitizer.sanitizeGraphiteMetricSegment("GET /index.html"));
	}
}
