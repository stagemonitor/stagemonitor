package org.stagemonitor.core;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.source.ConfigurationSource;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.HttpClient;

import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StagemonitorCoreConfigurationSourceInitializerTest {

	private StagemonitorCoreConfigurationSourceInitializer initializer = new StagemonitorCoreConfigurationSourceInitializer();
	final ConfigurationRegistry configuration = Mockito.mock(ConfigurationRegistry.class);
	final CorePlugin corePlugin = Mockito.mock(CorePlugin.class);

	@Before
	public void setUp() throws Exception {
		when(corePlugin.getElasticsearchConfigurationSourceProfiles()).thenReturn(Arrays.asList("test"));
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		ElasticsearchClient elasticsearchClient = new ElasticsearchClient(corePlugin, new HttpClient(), -1);
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient);
	}

	@Test(expected = IllegalStateException.class)
	public void testEsDownDeactivate() throws Exception {
		when(corePlugin.isDeactivateStagemonitorIfEsConfigSourceIsDown()).thenReturn(true);

		initializer.onConfigurationInitialized(new StagemonitorConfigurationSourceInitializer.ConfigInitializedArguments(configuration));
	}

	@Test
	public void testEsDown() throws Exception {
		when(corePlugin.isDeactivateStagemonitorIfEsConfigSourceIsDown()).thenReturn(false);

		initializer.onConfigurationInitialized(new StagemonitorConfigurationSourceInitializer.ConfigInitializedArguments(configuration));

		verify(configuration).addConfigurationSource(any(ConfigurationSource.class), eq(false));
	}
}
