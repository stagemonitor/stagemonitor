package org.stagemonitor.core;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.stagemonitor.configuration.ConfigurationRegistry;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StagemonitorTest {

	private static ConfigurationRegistry originalConfiguration;
	private ConfigurationRegistry configuration = mock(ConfigurationRegistry.class);
	private CorePlugin corePlugin = mock(CorePlugin.class);
	private Logger logger = mock(Logger.class);

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
		Stagemonitor.reset();
		Stagemonitor.setConfiguration(configuration);
		Stagemonitor.setLogger(logger);
		assertFalse(Stagemonitor.isStarted());
	}

	@After
	public void after() {
		Stagemonitor.reset();
	}

	@Test
	public void testStartMonitoring() throws Exception {
		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		Stagemonitor.setConfiguration(configuration);
		Stagemonitor.reset();

		final MeasurementSession measurementSession = new MeasurementSession("StagemonitorTest", "testHost", "testInstance");
		Stagemonitor.startMonitoring(measurementSession);
		Stagemonitor.startMonitoring(new MeasurementSession("StagemonitorTest2", "testHost2", "testInstance2"));

		assertTrue(Stagemonitor.isStarted());
		assertTrue(Stagemonitor.getMeasurementSession().isInitialized());
		assertSame(measurementSession, Stagemonitor.getMeasurementSession());
		verify(logger).info("Initializing plugin {}", "TestPlugin");
		verify(logger).info("Initializing plugin {}", "TestExceptionPlugin");
	}

	@Test
	public void testStartMonitoringNotActive() throws Exception {
		when(corePlugin.isStagemonitorActive()).thenReturn(false);

		final MeasurementSession measurementSession = new MeasurementSession("StagemonitorTest", "testHost", "testInstance");
		Stagemonitor.startMonitoring(measurementSession);

		assertTrue(Stagemonitor.isDisabled());
		assertFalse(Stagemonitor.isStarted());
		assertFalse(Stagemonitor.getMeasurementSession().isInitialized());
		verify(logger, times(0)).info("Initializing plugin {}", "TestPlugin");
		verify(logger, times(0)).info("Initializing plugin {}", "TestExceptionPlugin");
	}

	@Test
	public void testDisabledPlugin() throws Exception {
		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		when(corePlugin.getDisabledPlugins()).thenReturn(Arrays.asList("TestExceptionPlugin"));

		Stagemonitor.startMonitoring(new MeasurementSession("StagemonitorTest", "testHost", "testInstance"));

		verify(logger).info("Initializing plugin {}", "TestPlugin");
		verify(logger).info("Not initializing disabled plugin {}", "TestExceptionPlugin");
		verify(logger, times(0)).info("Initializing plugin {}", "TestExceptionPlugin");
	}

	@Test
	public void testNotInitialized() throws Exception {
		when(corePlugin.isStagemonitorActive()).thenReturn(true);

		final MeasurementSession measurementSession = new MeasurementSession(null, "testHost", "testInstance");
		Stagemonitor.startMonitoring(measurementSession);

		assertFalse(Stagemonitor.isStarted());
	}

}
