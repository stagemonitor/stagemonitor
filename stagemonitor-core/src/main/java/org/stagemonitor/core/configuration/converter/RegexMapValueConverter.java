package org.stagemonitor.core.configuration.converter;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegexMapValueConverter implements ValueConverter<Map<Pattern, String>> {

	@Override
	public Map<Pattern, String> convert(String s) throws IllegalArgumentException {
		if (s == null || s.trim().isEmpty()) {
			return Collections.emptyMap();
		}
		try {
			String[] groups = s.trim().split(",");
			Map<Pattern, String> pattenGroupMap = new LinkedHashMap<Pattern, String>(groups.length);

			for (String group : groups) {
				group = group.trim();
				String[] keyValue = group.split(":");
				if (keyValue.length != 2) {
					throw new IllegalArgumentException();
				}
				pattenGroupMap.put(Pattern.compile(keyValue[0].trim()), keyValue[1].trim());
			}
			return pattenGroupMap;
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Error while parsing pattern map. " +
					"Expected format <regex>: <name>[, <regex>: <name>]. Actual value: '" + s + ".'", e);
		}
	}

	@Override
	public String toString(Map<Pattern, String> value) {
		if (value == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (Iterator<Map.Entry<Pattern, String>> iterator = value.entrySet().iterator(); iterator.hasNext(); ) {
			Map.Entry<Pattern, String> entry = iterator.next();
			sb.append(entry.getKey().toString()).append(": ").append(entry.getValue());
			if (iterator.hasNext()) {
				sb.append(",\n");
			}
		}
		return sb.toString();
	}
}
