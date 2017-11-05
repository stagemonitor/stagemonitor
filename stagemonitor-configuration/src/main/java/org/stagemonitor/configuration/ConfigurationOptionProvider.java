package org.stagemonitor.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public abstract class ConfigurationOptionProvider {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public List<ConfigurationOption<?>> getConfigurationOptions() {
		List<ConfigurationOption<?>> configurationOptions = new LinkedList<ConfigurationOption<?>>();
		for (Field field : getAllDeclaredFields(getClass())) {
			if (ConfigurationOption.class.isAssignableFrom(field.getType())) {
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

	private static List<Field> getAllDeclaredFields(Class<?> type) {
		List<Field> fields = new ArrayList<Field>();
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
			fields.addAll(Arrays.asList(c.getDeclaredFields()));
		}
		return fields;
	}

}
