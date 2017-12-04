package org.stagemonitor.web.servlet.configuration;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.source.SimpleSource;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConfigurationPasswordCheckerTest {

	private final String passwordKey = "configuration.password";
	private ConfigurationRegistry configuration;
	private ConfigurationPasswordChecker configurationPasswordChecker;

	@Before
	public void init() {
		configuration = new ConfigurationRegistry(Collections.emptyList(),
				Collections.singletonList(new SimpleSource()));
		configurationPasswordChecker = new ConfigurationPasswordChecker(configuration, passwordKey);
	}

	@Test
	public void testIsPasswordSetTrue() throws Exception {
		ConfigurationRegistry configuration = new ConfigurationRegistry(Collections.emptyList(), Collections.emptyList());
		configuration.addConfigurationSource(SimpleSource.forTest(passwordKey, ""));
		assertTrue(new ConfigurationPasswordChecker(configuration, passwordKey).isPasswordSet());
	}

	@Test
	public void testIsPasswordSetFalse() throws Exception {
		ConfigurationRegistry configuration = new ConfigurationRegistry(Collections.emptyList(), Collections.emptyList());
		assertFalse(new ConfigurationPasswordChecker(configuration, passwordKey).isPasswordSet());
	}

	@Test
	public void testUpdateConfigurationWithoutPasswordSet() throws IOException {
		try {
			configurationPasswordChecker.assertPasswordCorrect(null);
			fail();
		} catch (IllegalStateException e) {
			assertEquals("'configuration.password' is not set.", e.getMessage());
		}
	}

	@Test
	public void testSetNewPasswordViaQueryParamsShouldFail() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(passwordKey, ""));
		try {
			configurationPasswordChecker.assertPasswordCorrect("");
			configuration.save(passwordKey, "", null);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Config key 'configuration.password' does not exist.", e.getMessage());
		}
	}

	@Test
	public void testUpdateConfigurationWithoutPassword() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(passwordKey, "pwd"));

		try {
			configurationPasswordChecker.assertPasswordCorrect(null);
			fail();
		} catch (IllegalStateException e) {
			assertEquals("Wrong password for 'configuration.password'.", e.getMessage());
		}

	}

	@Test
	public void testUpdateConfigurationWithPassword() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(passwordKey, "pwd"));
		configurationPasswordChecker.assertPasswordCorrect("pwd");
	}

}
