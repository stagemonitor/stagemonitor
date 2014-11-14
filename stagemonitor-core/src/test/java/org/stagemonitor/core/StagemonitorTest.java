package org.stagemonitor.core;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.stagemonitor.core.configuration.Configuration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StagemonitorTest {

	private Configuration configuration = mock(Configuration.class);
	private CorePlugin corePlugin = mock(CorePlugin.class);
	private Logger logger = mock(Logger.class);

	@Before
	public void before() {
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		Stagemonitor.reset();
		Stagemonitor.setConfiguration(configuration);
		Stagemonitor.setLogger(logger);
		assertFalse(Stagemonitor.isStarted());
		assertTrue(Stagemonitor.getMeasurementSession().isNull());
	}

	@After
	public void after() {
		Stagemonitor.reset();
	}

	@Test
	public void testStartMonitoring() throws Exception {
		when(corePlugin.isStagemonitorActive()).thenReturn(true);

		final MeasurementSession measurementSession = new MeasurementSession("testApp", "testHost", "testInstance");
		Stagemonitor.startMonitoring(measurementSession);
		Stagemonitor.startMonitoring(new MeasurementSession("testApp2", "testHost2", "testInstance2"));

		assertTrue(Stagemonitor.isStarted());
		assertTrue(Stagemonitor.getMeasurementSession().isInitialized());
		assertSame(measurementSession, Stagemonitor.getMeasurementSession());
		verify(logger).info("Initializing plugin {}", "TestPlugin");
		verify(logger).info("Initializing plugin {}", "TestExceptionPlugin");
	}

	@Test
	public void testStartMonitoringNotActive() throws Exception {
		when(corePlugin.isStagemonitorActive()).thenReturn(false);
		Stagemonitor.setConfiguration(configuration);

		final MeasurementSession measurementSession = new MeasurementSession("testApp", "testHost", "testInstance");
		Stagemonitor.startMonitoring(measurementSession);

		assertTrue(Stagemonitor.isDisabled());
		assertFalse(Stagemonitor.isStarted());
		assertFalse(Stagemonitor.getMeasurementSession().isInitialized());
		verify(logger, times(0)).info("Initializing plugin {}", "TestPlugin");
		verify(logger, times(0)).info("Initializing plugin {}", "TestExceptionPlugin");
	}

	@Test
	public void testDisabledPlugin() {
		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		when(corePlugin.getDisabledPlugins()).thenReturn(Arrays.asList("TestExceptionPlugin"));

		Stagemonitor.startMonitoring(new MeasurementSession("testApp", "testHost", "testInstance"));

		verify(logger).info("Initializing plugin {}", "TestPlugin");
		verify(logger).info("Not initializing disabled plugin {}", "TestExceptionPlugin");
		verify(logger, times(0)).info("Initializing plugin {}", "TestExceptionPlugin");
	}

	@Test
	public void testNotInitialized() {
		when(corePlugin.isStagemonitorActive()).thenReturn(true);

		final MeasurementSession measurementSession = new MeasurementSession(null, "testHost", "testInstance");
		Stagemonitor.startMonitoring(measurementSession);

		verify(logger).warn("Measurement Session is not initialized: {}", measurementSession);
	}

}
