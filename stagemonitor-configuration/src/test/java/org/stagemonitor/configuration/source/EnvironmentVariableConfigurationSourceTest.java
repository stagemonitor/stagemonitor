package org.stagemonitor.configuration.source;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public class EnvironmentVariableConfigurationSourceTest {

	@Test
	public void testGetValue() throws Exception {
		final EnvironmentVariableConfigurationSource source =
				new EnvironmentVariableConfigurationSource(Collections.singletonMap("STAGEMONITOR_TESTBLA123", "bingo"));
		Assert.assertEquals("bingo", source.getValue("stagemonitor.testBla123"));
	}

	@Test
	public void testGetName() throws Exception {
		final EnvironmentVariableConfigurationSource source = new EnvironmentVariableConfigurationSource();
		Assert.assertEquals("Environment Variables", source.getName());
	}
}
