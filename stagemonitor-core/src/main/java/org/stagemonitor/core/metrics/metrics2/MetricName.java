package org.stagemonitor.core.metrics.metrics2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.stagemonitor.core.util.GraphiteSanitizer;

/**
 * Represents a metrics 2.0 name that consists of a name and arbitrary tags (a set of key-value-pairs).
 * </p>
 * This is needed for example for InfluxDB's data model (see https://influxdb.com/docs/v0.9/concepts/schema_and_data_layout.html)
 * </p>
 * See also http://metrics20.org/
 */
public class MetricName {

	private final String name;

	private final LinkedHashMap<String, String> tags;

	private MetricName(String name, LinkedHashMap<String, String> tags) {
		this.name = name;
		this.tags = tags;
	}

	public MetricName withTags(Map<String, String> prefixTags) {
		return name(name).tags(prefixTags).tags(this.tags).build();
	}

	public static Builder name(String name) {
		return new Builder(name);
	}

	public static FluentLinkedHashMap tags(String key, String value) {
		final FluentLinkedHashMap tags = new FluentLinkedHashMap();
		tags.put(key, value);
		return tags;
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getTags() {
		return Collections.unmodifiableMap(tags);
	}

	/**
	 * Converts a metrics 2.0 name into a graphite compliant name by appending all tag values to the metric name
	 *
	 * @return A graphite compliant name
	 */
	public String toGraphiteName() {
		StringBuilder sb = new StringBuilder();
		for (String value : tags.values()) {
			sb.append(GraphiteSanitizer.sanitizeGraphiteMetricSegment(value)).append('.');
		}
		return sb.append(GraphiteSanitizer.sanitizeGraphiteMetricSegment(name)).toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MetricName)) return false;

		MetricName that = (MetricName) o;

		return name.equals(that.name) && tags.equals(that.tags);
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + tags.hashCode();
		return result;
	}

	public static class Builder {

		private final String name;

		private final LinkedHashMap<String, String> tags = new LinkedHashMap<String, String>(8);

		public Builder(String name) {
			this.name = name;
		}

		public Builder tag(String key, String value) {
			this.tags.put(key, value);
			return this;
		}

		public Builder tags(Map<String, String> tags) {
			this.tags.putAll(tags);
			return this;
		}

		public MetricName build() {
			return new MetricName(name, tags);
		}
	}

	public static class FluentLinkedHashMap extends LinkedHashMap<String, String> {

		public FluentLinkedHashMap addTag(String key, String value) {
			put(key, value);
			return this;
		}

		public FluentLinkedHashMap addTags(Map<String, String> tags) {
			putAll(tags);
			return this;
		}
	}

	@Override
	public String toString() {
		return "name='" + name + '\'' + ", tags=" + tags;
	}
}
