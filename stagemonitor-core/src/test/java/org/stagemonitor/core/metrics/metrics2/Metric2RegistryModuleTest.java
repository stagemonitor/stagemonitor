package org.stagemonitor.core.metrics.metrics2;

import static org.junit.Assert.assertEquals;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Gauge;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.util.JsonUtils;

public class Metric2RegistryModuleTest {

	private ObjectMapper mapper;
	private Metric2Registry registry;

	@Before
	public void setUp() throws Exception {
		mapper = JsonUtils.getMapper().copy().registerModule(new Metric2RegistryModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS));
		registry = new Metric2Registry();
	}

	@Test
	public void testCounter() throws Exception {
		registry.counter(name("foo").tag("bar", "baz").build()).inc();
		registry.counter(name("qux").tag("quux", "foo").build()).inc();
		assertEquals("[{\"name\":\"qux\",\"quux\":\"foo\",\"count\":1},{\"name\":\"foo\",\"bar\":\"baz\",\"count\":1}]", mapper.writeValueAsString(registry));
	}

	@Test
	public void testGauge() throws Exception {
		registry.register(name("foo").tag("bar", "baz").build(), new Gauge<Double>() {
			@Override
			public Double getValue() {
				return 1.1;
			}
		});
		assertEquals("[{\"name\":\"foo\",\"bar\":\"baz\",\"value\":1.1}]", mapper.writeValueAsString(registry));
	}
}