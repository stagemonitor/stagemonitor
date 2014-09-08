package org.stagemonitor.core.configuration;

import java.io.IOException;

public abstract class AbstractConfigurationSource implements ConfigurationSource {

	@Override
	public boolean isSavingPossible() {
		return false;
	}

	@Override
	public void save(String key, String value) throws IOException {
		throw new UnsupportedOperationException("Saving to " + getName() + " is not possible.");
	}

	@Override
	public void reload() {
	}
}
