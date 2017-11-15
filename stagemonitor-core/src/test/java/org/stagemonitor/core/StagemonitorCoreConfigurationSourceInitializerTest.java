package org.stagemonitor.core;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.source.SimpleSource;
import org.stagemonitor.core.configuration.ElasticsearchConfigurationSource;
import org.stagemonitor.core.configuration.SpringCloudConfigConfigurationSource;
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

		verify(configuration, never()).addConfigurationSourceAfter(any(SpringCloudConfigConfigurationSource.class), eq(SimpleSource.class));
		verify(configuration).addConfigurationSourceAfter(any(ElasticsearchConfigurationSource.class), eq(SimpleSource.class));
	}

	@Test
	public void testESDisabledAndSpringCloudEnabled() throws IOException {
		when(corePlugin.getSpringCloudConfigurationSourceProfiles()).thenReturn(Collections.singletonList("test"));
		when(corePlugin.getSpringCloudConfigServerAddress()).thenReturn("http://localhost/");
		when(corePlugin.getApplicationName()).thenReturn("myapplication");

		initializer.onConfigurationInitialized(new StagemonitorConfigurationSourceInitializer.ConfigInitializedArguments(configuration));

		verify(configuration).addConfigurationSourceAfter(any(SpringCloudConfigConfigurationSource.class), eq(SimpleSource.class));
		verify(configuration, never()).addConfigurationSourceAfter(any(ElasticsearchConfigurationSource.class), eq(SimpleSource.class));
	}

	@Test
	public void testSpringCloud_missingServerAddress() throws IOException {
		// Missing server address
		when(corePlugin.getSpringCloudConfigurationSourceProfiles()).thenReturn(Collections.singletonList("test"));
		when(corePlugin.getApplicationName()).thenReturn("myapplication");

		initializer.onConfigurationInitialized(new StagemonitorConfigurationSourceInitializer.ConfigInitializedArguments(configuration));

		verify(configuration, never()).addConfigurationSourceAfter(any(SpringCloudConfigConfigurationSource.class), eq(SimpleSource.class));
	}

	@Test
	public void testSpringCloud_badServerAddress() throws IOException {
		when(corePlugin.getSpringCloudConfigurationSourceProfiles()).thenReturn(Collections.singletonList("test"));
		when(corePlugin.getSpringCloudConfigServerAddress()).thenReturn("some.invalid.server/address/");
		when(corePlugin.getApplicationName()).thenReturn("myapplication");

		initializer.onConfigurationInitialized(new StagemonitorConfigurationSourceInitializer.ConfigInitializedArguments(configuration));

		verify(configuration, never()).addConfigurationSourceAfter(any(SpringCloudConfigConfigurationSource.class), eq(SimpleSource.class));
	}

	@Test
	public void testSpringCloud_nonDefaultOrMissingApplicationName() throws IOException {
		when(corePlugin.getSpringCloudConfigurationSourceProfiles()).thenReturn(Collections.singletonList("test"));
		when(corePlugin.getSpringCloudConfigServerAddress()).thenReturn("some.invalid.server/address/");
		// Making use of the default application name here

		initializer.onConfigurationInitialized(new StagemonitorConfigurationSourceInitializer.ConfigInitializedArguments(configuration));

		verify(configuration, never()).addConfigurationSourceAfter(any(SpringCloudConfigConfigurationSource.class), eq(SimpleSource.class));
	}

	@Test
	public void testCorrectProperties() throws IOException {
		when(corePlugin.getSpringCloudConfigurationSourceProfiles()).thenReturn(Collections.singletonList("test"));
		when(corePlugin.getSpringCloudConfigServerAddress()).thenReturn("http://localhost/");
		when(corePlugin.getApplicationName()).thenReturn("myapplication");

		initializer.onConfigurationInitialized(new StagemonitorConfigurationSourceInitializer.ConfigInitializedArguments(configuration));

		ArgumentCaptor<SpringCloudConfigConfigurationSource> configSourceCaptor = ArgumentCaptor.forClass(SpringCloudConfigConfigurationSource.class);
		verify(configuration).addConfigurationSourceAfter(configSourceCaptor.capture(), eq(SimpleSource.class));

		Assert.assertEquals("myapplication", configSourceCaptor.getValue().getApplicationName());
		Assert.assertEquals("test", configSourceCaptor.getValue().getProfile());
	}

	@Test
	public void testSpringCloud_defaultProfile() throws IOException {
		when(corePlugin.getSpringCloudConfigServerAddress()).thenReturn("http://localhost/");
		when(corePlugin.getApplicationName()).thenReturn("myapplication");

		initializer.onConfigurationInitialized(new StagemonitorConfigurationSourceInitializer.ConfigInitializedArguments(configuration));

		ArgumentCaptor<SpringCloudConfigConfigurationSource> configSourceCaptor = ArgumentCaptor.forClass(SpringCloudConfigConfigurationSource.class);
		verify(configuration).addConfigurationSourceAfter(configSourceCaptor.capture(), eq(SimpleSource.class));

		// Expecting it to use the DEFAULT_PROFILE
		Assert.assertEquals(SpringCloudConfigConfigurationSource.DEFAULT_PROFILE, configSourceCaptor.getValue().getProfile());
	}

	@Test
	public void testSpringCloud_multipleProfiles() throws IOException {
		when(corePlugin.getSpringCloudConfigurationSourceProfiles()).thenReturn(Arrays.asList("common", "prod", "local"));
		when(corePlugin.getSpringCloudConfigServerAddress()).thenReturn("http://localhost/");
		when(corePlugin.getApplicationName()).thenReturn("myapplication");

		initializer.onConfigurationInitialized(new StagemonitorConfigurationSourceInitializer.ConfigInitializedArguments(configuration));

		// Expecting 3 config source
		verify(configuration, times(3)).addConfigurationSourceAfter(any(SpringCloudConfigConfigurationSource.class), eq(SimpleSource.class));
	}
}
