package org.stagemonitor.alerting;

import org.junit.Test;
import org.stagemonitor.web.servlet.initializer.ServletContainerInitializerUtil;
import org.stagemonitor.web.servlet.initializer.StagemonitorServletContainerInitializer;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class AlertingServletContainerInitializerTest {

	@Test
	public void testServletContainerInitializer() throws Exception {
		assertThat(ServletContainerInitializerUtil.getStagemonitorSCIs()
				.stream()
				.map(StagemonitorServletContainerInitializer::getClass)
				.collect(Collectors.toList()))
				.contains(AlertingPlugin.Initializer.class);
	}
}
