package org.stagemonitor.core.metrics.metrics2;

import static org.junit.Assert.assertEquals;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import org.junit.Test;

public class MetricNameTest {

	@Test
	public void testGetInfluxDbStringOrderedTags() throws Exception {
		assertEquals("cpu_usage,core=1,level=user",
				name("cpu_usage").tag("level", "user").tag("core", "1").build().getInfluxDbLineProtocolString());
	}

	@Test
	public void testGetInfluxDbStringWhiteSpace() throws Exception {
		assertEquals("cpu\\ usage,level=soft\\ irq",
				name("cpu usage").tag("level", "soft irq").build().getInfluxDbLineProtocolString());
	}

	@Test
	public void testGetInfluxDbStringNoTags() throws Exception {
		assertEquals("cpu_usage",
				name("cpu_usage").build().getInfluxDbLineProtocolString());
	}

	@Test
	public void testGetInfluxDbStringAllEscapingAndQuotingBehavior() throws Exception {
		assertEquals("\"measurement\\ with\\ quotes\",tag\\ key\\ with\\ spaces=tag\\,value\\,with\"commas\"",
				name("\"measurement with quotes\"").tag("tag key with spaces", "tag,value,with\"commas\"").build().getInfluxDbLineProtocolString());
	}
}