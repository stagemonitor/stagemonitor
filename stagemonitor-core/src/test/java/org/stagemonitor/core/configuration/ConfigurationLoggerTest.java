package org.stagemonitor.core.configuration;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationOptionProvider;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.source.SimpleSource;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ConfigurationLoggerTest {

	private ConfigurationLogger configurationLogger;
	private Logger logger;

	@Before
	public void setUp() throws Exception {
		logger = mock(Logger.class);
		configurationLogger = new ConfigurationLogger(logger);
	}

	@Test
	public void testDeprecatedOption() throws Exception {
		class Provider extends ConfigurationOptionProvider {
			private ConfigurationOption<String> deprecatedOption = ConfigurationOption.stringOption()
					.key("foo")
					.tags("deprecated")
					.buildRequired();
		}
		final ConfigurationRegistry configurationRegistry = new ConfigurationRegistry(
				Collections.singletonList(new Provider()),
				Collections.singletonList(new SimpleSource().add("foo", "bar"))
		);
		configurationLogger.logConfiguration(configurationRegistry);

		verify(logger).warn(contains("Detected usage of deprecated configuration option '{}'"), eq("foo"));
	}

	@Test
	public void testAliasKey() throws Exception {
		class Provider extends ConfigurationOptionProvider {
			private ConfigurationOption<String> aliasOption = ConfigurationOption.stringOption()
					.key("foo")
					.aliasKeys("foo.old")
					.buildRequired();
		}
		final ConfigurationRegistry configurationRegistry = new ConfigurationRegistry(
				Collections.singletonList(new Provider()),
				Collections.singletonList(new SimpleSource().add("foo.old", "bar"))
		);
		configurationLogger.logConfiguration(configurationRegistry);

		verify(logger).warn(eq("Detected usage of an old configuration key: '{}'. " +
				"Please use '{}' instead."), eq("foo.old"), eq("foo"));
	}

	@Test
	public void testLogSensitive() throws Exception {
		class Provider extends ConfigurationOptionProvider {
			private ConfigurationOption<String> sensitiveOption = ConfigurationOption.stringOption()
					.key("foo")
					.sensitive()
					.buildRequired();
		}
		final ConfigurationRegistry configurationRegistry = new ConfigurationRegistry(
				Collections.singletonList(new Provider()),
				Collections.singletonList(new SimpleSource("source").add("foo", "secret"))
		);
		configurationLogger.logConfiguration(configurationRegistry);

		verify(logger).info(startsWith("# stagemonitor configuration"));
		verify(logger).info(eq("{}: {} (source: {})"), eq("foo"), eq("XXXX"), eq("source"));
	}

	@Test
	public void testLogUrlWithBasicAuth() throws Exception {
		class Provider extends ConfigurationOptionProvider {
			private ConfigurationOption<List<URL>> urlBasicAuthOption = ConfigurationOption.urlsOption()
					.key("foo")
					.buildRequired();
		}
		final ConfigurationRegistry configurationRegistry = new ConfigurationRegistry(
				Collections.singletonList(new Provider()),
				Collections.singletonList(new SimpleSource("source").add("foo", "http://user:pwd@example.com"))
		);
		configurationLogger.logConfiguration(configurationRegistry);

		verify(logger).info(startsWith("# stagemonitor configuration"));
		verify(logger).info(eq("{}: {} (source: {})"), eq("foo"), eq("http://user:XXX@example.com"), eq("source"));
	}
}
