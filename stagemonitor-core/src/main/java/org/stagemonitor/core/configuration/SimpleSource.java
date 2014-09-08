package org.stagemonitor.core.configuration;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A map based configuration source
 */
public class SimpleSource extends AbstractConfigurationSource {

	private final ConcurrentMap<String, String> config = new ConcurrentHashMap<String, String>();
	private final String name;

	public SimpleSource() {
		this("Transient Configuration Source");
	}

	public SimpleSource(String name) {
		this.name = name;
	}

	public static SimpleSource forTest(String key, String value) {
		final SimpleSource simpleConfig = new SimpleSource("Test Configuration Source");
		simpleConfig.add(key, value);
		return simpleConfig;
	}

	@Override
	public String getValue(String key) {
		return config.get(key);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isSavingPossible() {
		return true;
	}

	@Override
	public void save(String key, String value) throws IOException {
		config.put(key, value);
	}

	public SimpleSource add(String key, String value) {
		config.put(key, value);
		return this;
	}
}
