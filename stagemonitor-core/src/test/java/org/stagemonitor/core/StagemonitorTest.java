package org.stagemonitor.core;

import com.codahale.metrics.health.HealthCheckRegistry;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationRegistry;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StagemonitorTest {

	private static ConfigurationRegistry originalConfiguration;
	private final HealthCheckRegistry healthCheckRegistry = Stagemonitor.getHealthCheckRegistry();
	private ConfigurationRegistry configuration = mock(ConfigurationRegistry.class);
	private CorePlugin corePlugin = mock(CorePlugin.class);

	@BeforeClass
	public static void beforeClass() {
		originalConfiguration = Stagemonitor.getConfiguration();
	}

	@AfterClass
	public static void afterClass() {
		Stagemonitor.setConfiguration(originalConfiguration);
		Stagemonitor.reset();
	}

	@Before
	public void before() {
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		Stagemonitor.reset();
		Stagemonitor.setConfiguration(configuration);
//		assertFalse(Stagemonitor.isStarted());
		for (String name : healthCheckRegistry.getNames()) {
			healthCheckRegistry.unregister(name);
		}
	}

	@After
	public void after() {
		Stagemonitor.reset();
		for (String name : healthCheckRegistry.getNames()) {
			healthCheckRegistry.unregister(name);
		}
	}

	@Test
	public void testStartMonitoring() throws Exception {
		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		Stagemonitor.setConfiguration(configuration);
		Stagemonitor.reset();

		final MeasurementSession measurementSession = new MeasurementSession("StagemonitorTest", "testHost", "testInstance");
		Stagemonitor.reset(measurementSession);

		assertTrue(Stagemonitor.isStarted());
		assertTrue(Stagemonitor.getMeasurementSession().isInitialized());
		assertSame(measurementSession, Stagemonitor.getMeasurementSession());
		assertThat(healthCheckRegistry.runHealthCheck("TestPlugin").isHealthy()).isTrue();
		assertThat(healthCheckRegistry.runHealthCheck("TestExceptionPlugin").isHealthy()).isFalse();
	}

	@Test
	public void testStartMonitoringNotActive() throws Exception {
		when(corePlugin.isStagemonitorActive()).thenReturn(false);

		final MeasurementSession measurementSession = new MeasurementSession("StagemonitorTest", "testHost", "testInstance");
		Stagemonitor.reset(measurementSession);

		assertTrue(Stagemonitor.isDisabled());
		assertFalse(Stagemonitor.isStarted());
		assertTrue(Stagemonitor.getMeasurementSession().isInitialized());
		assertThat(healthCheckRegistry.getNames()).doesNotContain("TestPlugin", "TestExceptionPlugin");
	}

	@Test
	public void testDisabledPlugin() throws Exception {
		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		when(corePlugin.getDisabledPlugins()).thenReturn(Collections.singletonList("TestExceptionPlugin"));

		Stagemonitor.reset(new MeasurementSession("StagemonitorTest", "testHost", "testInstance"));

		assertThat(healthCheckRegistry.runHealthCheck("TestPlugin").isHealthy()).isTrue();
		assertThat(healthCheckRegistry.runHealthCheck("TestExceptionPlugin").isHealthy()).isFalse();
		assertThat(healthCheckRegistry.runHealthCheck("TestExceptionPlugin").getMessage()).isEqualTo("disabled via configuration");
	}

	@Test
	public void testNotInitialized() throws Exception {
		when(corePlugin.isStagemonitorActive()).thenReturn(true);

		final MeasurementSession measurementSession = new MeasurementSession(null, "testHost", "testInstance");
		Stagemonitor.reset(measurementSession);

		assertFalse(Stagemonitor.isStarted());
	}

	public static class PluginNoDependency extends StagemonitorPlugin {
		public List<Class<? extends StagemonitorPlugin>> dependsOn() {
			return Collections.emptyList();
		}
	}

	public static class PluginSimpleDependency extends StagemonitorPlugin {
		@Override
		public List<Class<? extends StagemonitorPlugin>> dependsOn() {
			return Collections.singletonList(PluginNoDependency.class);
		}
	}

	public static class PluginSimpleDependency2 extends StagemonitorPlugin {
		@Override
		public List<Class<? extends StagemonitorPlugin>> dependsOn() {
			return Collections.singletonList(PluginNoDependency.class);
		}
	}

	public static class PluginMultipleDependencies extends StagemonitorPlugin {
		@Override
		public List<Class<? extends StagemonitorPlugin>> dependsOn() {
			return Arrays.asList(PluginSimpleDependency.class, PluginSimpleDependency2.class, PluginNoDependency.class);
		}
	}

	public static class PluginCyclicA extends StagemonitorPlugin {
		@Override
		public List<Class<? extends StagemonitorPlugin>> dependsOn() {
			return Collections.singletonList(PluginCyclicB.class);
		}
	}
	public static class PluginCyclicB extends StagemonitorPlugin {
		@Override
		public List<Class<? extends StagemonitorPlugin>> dependsOn() {
			return Collections.singletonList(PluginCyclicA.class);
		}
	}

	@Test
	public void testInitPluginsCyclicDependency() {
		final List<StagemonitorPlugin> plugins = Arrays.asList(new PluginNoDependency(), new PluginCyclicA(), new PluginCyclicB());
		assertThatThrownBy(() -> Stagemonitor.initializePluginsInOrder(Collections.emptyList(), plugins))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("PluginCyclicA")
				.hasMessageContaining("PluginCyclicB");
	}

	@Test
	public void testInitPlugins_NoDependency() {
		final PluginNoDependency pluginNoDependency = new PluginNoDependency();
		Stagemonitor.initializePluginsInOrder(Collections.emptyList(), Collections.singletonList(pluginNoDependency));
		assertThat(pluginNoDependency.isInitialized()).isTrue();
	}

	@Test
	public void testInitPlugins_SimpleDependency() {
		final PluginNoDependency pluginNoDependency = new PluginNoDependency();
		final PluginSimpleDependency pluginSimpleDependency = new PluginSimpleDependency();
		Stagemonitor.initializePluginsInOrder(Collections.emptyList(), Arrays.asList(pluginNoDependency, pluginSimpleDependency));
		assertThat(pluginNoDependency.isInitialized()).isTrue();
		assertThat(pluginSimpleDependency.isInitialized()).isTrue();
	}

	@Test
	public void testInitPlugins_MultipleDependencies() {
		final List<StagemonitorPlugin> plugins = Arrays.asList(new PluginMultipleDependencies(), new PluginSimpleDependency2(),
				new PluginNoDependency(), new PluginSimpleDependency());
		Stagemonitor.initializePluginsInOrder(Collections.emptyList(), plugins);
		for (StagemonitorPlugin plugin : plugins) {
			assertThat(plugin.isInitialized()).describedAs("{} is not initialized", plugin.getClass().getSimpleName()).isTrue();
		}
	}

}
