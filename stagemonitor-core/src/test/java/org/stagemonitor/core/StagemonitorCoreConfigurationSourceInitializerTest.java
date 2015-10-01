package org.stagemonitor.core;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.source.ConfigurationSource;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class StagemonitorCoreConfigurationSourceInitializerTest {

	private StagemonitorCoreConfigurationSourceInitializer initializer = new StagemonitorCoreConfigurationSourceInitializer();
	final Configuration configuration = Mockito.mock(Configuration.class);
	final CorePlugin corePlugin = Mockito.mock(CorePlugin.class);

	@Before
	public void setUp() throws Exception {
		when(corePlugin.getElasticsearchConfigurationSourceProfiles()).thenReturn(Arrays.asList("test"));
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		ElasticsearchClient elasticsearchClient = new ElasticsearchClient(corePlugin);
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient);
	}

	@Test(expected = IllegalStateException.class)
	public void testEsDownDeactivate() throws Exception {
		when(corePlugin.isDeactivateStagemonitorIfEsConfigSourceIsDown()).thenReturn(true);

		initializer.onConfigurationInitialized(configuration);
	}

	@Test
	public void testEsDown() throws Exception {
		when(corePlugin.isDeactivateStagemonitorIfEsConfigSourceIsDown()).thenReturn(false);

		initializer.onConfigurationInitialized(configuration);

		verify(configuration).addConfigurationSource(any(ConfigurationSource.class), eq(false));
	}
}
