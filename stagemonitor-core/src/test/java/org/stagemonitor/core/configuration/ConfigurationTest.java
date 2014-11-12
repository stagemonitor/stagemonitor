package org.stagemonitor.core.configuration;

import java.io.IOException;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.source.ConfigurationSource;
import org.stagemonitor.core.configuration.source.SimpleSource;
import org.stagemonitor.core.configuration.source.SystemPropertyConfigurationSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.Stagemonitor.STAGEMONITOR_PASSWORD;

public class ConfigurationTest {

	private Configuration configuration;
	private CorePlugin corePlugin;

	@Before
	public void init() {
		configuration = new Configuration(Collections.singletonList(new CorePlugin()),
				Collections.<ConfigurationSource>singletonList(new SimpleSource()), STAGEMONITOR_PASSWORD);
		corePlugin = configuration.getConfig(CorePlugin.class);
	}

	@Test
	public void testUpdateConfigurationWithoutPasswordSet() throws IOException {
		assertFalse(corePlugin.isInternalMonitoringActive());
		try {
			configuration.save("stagemonitor.internal.monitoring", "true", "Transient Configuration Source", null);
			fail();
		} catch (IllegalStateException e) {
			assertEquals("Update configuration password is not set. Dynamic configuration changes are therefore not allowed.", e.getMessage());
		}
		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfiguration() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		assertFalse(corePlugin.isInternalMonitoringActive());

		configuration.save("stagemonitor.internal.monitoring", "true", "Transient Configuration Source", null);

		assertTrue(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationWrongConfigurationSource() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		assertFalse(corePlugin.isInternalMonitoringActive());
		try {
			configuration.save("stagemonitor.internal.monitoring", "true", "foo", null);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Configuration source 'foo' does not exist.", e.getMessage());
		}
		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationNotSaveableConfigurationSource() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		configuration.addConfigurationSource(new SystemPropertyConfigurationSource());
		assertFalse(corePlugin.isInternalMonitoringActive());
		try {
			configuration.save("stagemonitor.internal.monitoring", "true", "Java System Properties", null);
			fail();
		} catch (UnsupportedOperationException e) {
			assertEquals("Saving to Java System Properties is not possible.", e.getMessage());
		}
		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationNonDynamicTransient() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		assertEquals(60, corePlugin.getConsoleReportingInterval());
		try {
			configuration.save("stagemonitor.reporting.interval.console", "1", "Transient Configuration Source", null);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Non dynamic options can't be saved to a transient configuration source.", e.getMessage());
		}
		assertEquals(60, corePlugin.getConsoleReportingInterval());
	}

	@Test
	public void testUpdateConfigurationNonDynamicPersistent() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		final ConfigurationSource persistentSourceMock = mock(ConfigurationSource.class);
		when(persistentSourceMock.isSavingPossible()).thenReturn(true);
		when(persistentSourceMock.isSavingPersistent()).thenReturn(true);
		when(persistentSourceMock.getName()).thenReturn("Test Persistent");
		configuration.addConfigurationSource(persistentSourceMock);
		assertEquals(60, corePlugin.getConsoleReportingInterval());

		configuration.save("stagemonitor.reporting.interval.console", "1", "Test Persistent", null);

		verify(persistentSourceMock).save("stagemonitor.reporting.interval.console", "1");
	}

	@Test
	public void testSetNewPasswordViaQueryParamsShouldFail() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		try {
			configuration.save(STAGEMONITOR_PASSWORD, "pwd", "Transient Configuration Source", null);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Config key 'stagemonitor.password' does not exist.", e.getMessage());
		}
	}

	@Test
	public void testUpdateConfigurationWithoutPassword() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, "pwd"));
		assertFalse(corePlugin.isInternalMonitoringActive());

		try {
			configuration.save("stagemonitor.internal.monitoring", "true", "Transient Configuration Source", null);
			fail();
		} catch (IllegalStateException e) {
			assertEquals("Wrong password for updating configuration.", e.getMessage());
		}

		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationWithPassword() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, "pwd"));
		assertFalse(corePlugin.isInternalMonitoringActive());

		configuration.save("stagemonitor.internal.monitoring", "true", "Transient Configuration Source", "pwd");

		assertTrue(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testAddConfigurationSource() {
		Configuration configuration = new Configuration("");
		configuration.addConfigurationSource(new SimpleSource());

		assertEquals(Collections.singletonMap("Transient Configuration Source", true), configuration.getNamesOfConfigurationSources());
	}

	@Test
	public void testIsPasswordSetTrue() throws Exception {
		Configuration configuration = new Configuration("pwd");
		configuration.addConfigurationSource(SimpleSource.forTest("pwd", ""));
		assertTrue(configuration.isPasswordSet());
	}

	@Test
	public void testIsPasswordSetFalse() throws Exception {
		Configuration configuration = new Configuration("pwd");
		assertFalse(configuration.isPasswordSet());
	}
}
