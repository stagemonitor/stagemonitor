package org.stagemonitor.core.configuration;

import java.util.HashMap;

/**
 * A configuration source for tests
 */
public class SimpleSource implements ConfigurationSource {

	private final HashMap<String, String> config = new HashMap<String, String>();

	public static SimpleSource of(String key, String value) {
		final SimpleSource simpleConfig = new SimpleSource();
		simpleConfig.add(key, value);
		return simpleConfig;
	}

	@Override
	public String getValue(String key) {
		return config.get(key);
	}

	@Override
	public void reload() {
	}

	@Override
	public String getName() {
		return "Simple Configuration Source";
	}

	public SimpleSource add(String key, String value) {
		config.put(key, value);
		return this;
	}
}
