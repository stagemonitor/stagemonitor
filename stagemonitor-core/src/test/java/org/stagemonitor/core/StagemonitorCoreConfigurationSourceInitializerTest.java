package org.stagemonitor.core;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.source.SimpleSource;
import org.stagemonitor.core.configuration.ElasticsearchConfigurationSource;
import org.stagemonitor.core.configuration.RemotePropertiesConfigurationSource;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.HttpClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StagemonitorCoreConfigurationSourceInitializerTest {

	private StagemonitorCoreConfigurationSourceInitializer initializer = new StagemonitorCoreConfigurationSourceInitializer();
	private final ConfigurationRegistry configuration = Mockito.mock(ConfigurationRegistry.class);
	private final CorePlugin corePlugin = Mockito.mock(CorePlugin.class);

	@Before
	public void setUp() throws Exception {
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
	}

	private void prepareESTest() {
		when(corePlugin.getElasticsearchConfigurationSourceProfiles()).thenReturn(Collections.singletonList("test"));
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		ElasticsearchClient elasticsearchClient = new ElasticsearchClient(corePlugin, new HttpClient(), -1);
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient);
	}

	@Test(expected = IllegalStateException.class)
	public void testEsDownDeactivate() throws Exception {
		prepareESTest();
		when(corePlugin.isDeactivateStagemonitorIfEsConfigSourceIsDown()).thenReturn(true);

		initializer.onConfigurationInitialized(new StagemonitorConfigurationSourceInitializer.ConfigInitializedArguments(configuration));
	}

	@Test
	public void testEsDown() throws Exception {
		prepareESTest();
		when(corePlugin.isDeactivateStagemonitorIfEsConfigSourceIsDown()).thenReturn(false);

		initializer.onConfigurationInitialized(new StagemonitorConfigurationSourceInitializer.ConfigInitializedArguments(configuration));

		verify(configuration).addConfigurationSourceAfter(any(ElasticsearchConfigurationSource.class), eq(SimpleSource.class));
	}

	@Test
	public void testESEnabledAndSpringCloudDisabled() throws IOException {
		prepareESTest();

		initializer.onConfigurationInitialized(new StagemonitorConfigurationSourceInitializer.ConfigInitializedArguments(configuration));

		verify(configuration, never()).addConfigurationSourceAfter(any(RemotePropertiesConfigurationSource.class), eq(SimpleSource.class));
		verify(configuration).addConfigurationSourceAfter(any(ElasticsearchConfigurationSource.class), eq(SimpleSource.class));
	}

	@Test
	public void testESDisabledAndSpringCloudEnabled() throws IOException {
		when(corePlugin.getRemotePropertiesConfigUrls()).thenReturn(Collections.singletonList("http://localhost/config.json"));

		initializer.onConfigurationInitialized(new StagemonitorConfigurationSourceInitializer.ConfigInitializedArguments(configuration));

		verify(configuration).addConfigurationSourceAfter(any(RemotePropertiesConfigurationSource.class), eq(SimpleSource.class));
		verify(configuration, never()).addConfigurationSourceAfter(any(ElasticsearchConfigurationSource.class), eq(SimpleSource.class));
	}

	@Test
	public void testSpringCloud_missingServerAddress() throws IOException {
		initializer.onConfigurationInitialized(new StagemonitorConfigurationSourceInitializer.ConfigInitializedArguments(configuration));

		verify(configuration, never()).addConfigurationSourceAfter(any(RemotePropertiesConfigurationSource.class), eq(SimpleSource.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSpringCloud_badServerAddress() throws IOException {
		when(corePlugin.getRemotePropertiesConfigUrls()).thenReturn(Collections.singletonList("some.invalid.server/address/"));

		initializer.onConfigurationInitialized(new StagemonitorConfigurationSourceInitializer.ConfigInitializedArguments(configuration));

		verify(configuration, never()).addConfigurationSourceAfter(any(RemotePropertiesConfigurationSource.class), eq(SimpleSource.class));
	}

	@Test
	public void testCorrectProperties() throws IOException {
		when(corePlugin.getRemotePropertiesConfigUrls()).thenReturn(Collections.singletonList("http://localhost/config.json"));

		initializer.onConfigurationInitialized(new StagemonitorConfigurationSourceInitializer.ConfigInitializedArguments(configuration));

		ArgumentCaptor<RemotePropertiesConfigurationSource> configSourceCaptor = ArgumentCaptor.forClass(RemotePropertiesConfigurationSource.class);
		verify(configuration).addConfigurationSourceAfter(configSourceCaptor.capture(), eq(SimpleSource.class));

		Assert.assertEquals("http://localhost/config.json", configSourceCaptor.getValue().getName());
	}

	@Test
	public void testSpringCloud_multipleConfigUrls() throws IOException {
		when(corePlugin.getRemotePropertiesConfigUrls()).thenReturn(
				Arrays.asList("http://localhost/config1", "http://localhost/config2", "http://some.other/domain"));
		when(corePlugin.getApplicationName()).thenReturn("myapplication");

		initializer.onConfigurationInitialized(new StagemonitorConfigurationSourceInitializer.ConfigInitializedArguments(configuration));

		// Expecting 3 config source
		verify(configuration, times(3)).addConfigurationSourceAfter(any(RemotePropertiesConfigurationSource.class), eq(SimpleSource.class));
	}
}
