package org.stagemonitor.web.init;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.stagemonitor.core.Stagemonitor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SpringBootWebPluginInitializerTest {

	private EmbeddedServletContainerCustomizerBeanPostProcessor postProcessor;

	@Before
	public void setUp() throws Exception {
		Stagemonitor.init();
		postProcessor = new EmbeddedServletContainerCustomizerBeanPostProcessor();
	}

	@Test
	public void addInitializer() throws Exception {
		final ConfigurableEmbeddedServletContainer mock = mock(ConfigurableEmbeddedServletContainer.class);
		postProcessor.setApplicationContext(mock(ApplicationContext.class));

		postProcessor.postProcessBeforeInitialization(mock, null);

		verify(mock).addInitializers(any(SpringBootWebPluginInitializer.StagemonitorServletContextInitializer.class));
	}

}
