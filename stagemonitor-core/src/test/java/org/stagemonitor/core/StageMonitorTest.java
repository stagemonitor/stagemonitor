package org.stagemonitor.core;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class StageMonitorTest {

	private Configuration configuration = mock(Configuration.class);
	private Logger logger = mock(Logger.class);

	@Before
	public void before() {
		StageMonitor.reset();
		StageMonitor.setConfiguration(configuration);
		StageMonitor.setLogger(logger);
		assertFalse(StageMonitor.isStarted());
		assertTrue(StageMonitor.getMeasurementSession().isNull());
	}

	@After
	public void after() {
		StageMonitor.reset();
	}

	@Test
	public void testStartMonitoring() throws Exception {
		when(configuration.isStagemonitorActive()).thenReturn(true);

		final MeasurementSession measurementSession = new MeasurementSession("testApp", "testHost", "testInstance");
		StageMonitor.startMonitoring(measurementSession);
		StageMonitor.startMonitoring(new MeasurementSession("testApp2", "testHost2", "testInstance2"));

		assertTrue(StageMonitor.isStarted());
		assertTrue(StageMonitor.getMeasurementSession().isInitialized());
		assertSame(measurementSession, StageMonitor.getMeasurementSession());
		verify(logger).info("Initializing plugin {}", "TestPlugin");
		verify(logger).info("Initializing plugin {}", "TestExceptionPlugin");
	}

	@Test
	public void testStartMonitoringNotActive() throws Exception {
		when(configuration.isStagemonitorActive()).thenReturn(false);
		StageMonitor.setConfiguration(configuration);

		final MeasurementSession measurementSession = new MeasurementSession("testApp", "testHost", "testInstance");
		StageMonitor.startMonitoring(measurementSession);

		assertTrue(StageMonitor.isStarted());
		assertFalse(StageMonitor.getMeasurementSession().isInitialized());
		verify(logger, times(0)).info("Initializing plugin {}", "TestPlugin");
		verify(logger, times(0)).info("Initializing plugin {}", "TestExceptionPlugin");
	}

	@Test
	public void testDisabledPlugin() {
		when(configuration.isStagemonitorActive()).thenReturn(true);
		when(configuration.getDisabledPlugins()).thenReturn(Arrays.asList("TestExceptionPlugin"));

		StageMonitor.startMonitoring(new MeasurementSession("testApp", "testHost", "testInstance"));

		verify(logger).info("Initializing plugin {}", "TestPlugin");
		verify(logger).info("Not initializing disabled plugin {}", "TestExceptionPlugin");
		verify(logger, times(0)).info("Initializing plugin {}", "TestExceptionPlugin");
	}

	@Test
	public void testNotInitialized() {
		when(configuration.isStagemonitorActive()).thenReturn(true);

		final MeasurementSession measurementSession = new MeasurementSession(null, "testHost", "testInstance");
		StageMonitor.startMonitoring(measurementSession);

		verify(logger).warn("Measurement Session is not initialized: {}", measurementSession);
	}

}
