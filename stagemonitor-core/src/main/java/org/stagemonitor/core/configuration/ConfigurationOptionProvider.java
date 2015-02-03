package org.stagemonitor.core.configuration;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConfigurationOptionProvider {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public List<ConfigurationOption<?>> getConfigurationOptions() {
		List<ConfigurationOption<?>> configurationOptions = new LinkedList<ConfigurationOption<?>>();
		for (Field field : getClass().getDeclaredFields()) {
			if (field.getType() == ConfigurationOption.class) {
				field.setAccessible(true);
				try {
					configurationOptions.add((ConfigurationOption) field.get(this));
				} catch (IllegalAccessException e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}
		return configurationOptions;
	}

}
