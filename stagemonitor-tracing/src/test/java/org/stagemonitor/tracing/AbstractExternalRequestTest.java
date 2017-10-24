package org.stagemonitor.tracing;

import org.junit.Test;

import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractExternalRequestTest {

	@Test
	public void testDontMonitorClientRootSpans() throws Exception {
		MockTracer tracer = new MockTracer();
		new AbstractExternalRequest(tracer) {
			@Override
			protected String getType() {
				return "jdbc";
			}
		}.createScope().close();

		assertThat(tracer.finishedSpans().get(0).tags().get(Tags.SAMPLING_PRIORITY.getKey())).isEqualTo(0);
	}

}
