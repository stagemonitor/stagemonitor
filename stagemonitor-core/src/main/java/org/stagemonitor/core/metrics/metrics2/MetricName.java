package org.stagemonitor.core.metrics.metrics2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.stagemonitor.core.util.GraphiteSanitizer;

/**
 * Represents a metrics 2.0 name that consists of a name and arbitrary tags (a set of key-value-pairs).
 * </p>
 * This is needed for example for InfluxDB's data model (see https://influxdb.com/docs/v0.9/concepts/schema_and_data_layout.html)
 * </p>
 * See also http://metrics20.org/
 */
public class MetricName {

	private String influxDbLineProtocolString;
	private int hashCode;

	private final String name;

	private final List<String> tagKeys;
	private final List<String> tagValues;

	private MetricName(String name, List<String> tagKeys, List<String> tagValues) {
		this.name = name;
		this.tagKeys = Collections.unmodifiableList(tagKeys);
		this.tagValues = Collections.unmodifiableList(tagValues);
	}

	public MetricName withTag(String key, String value) {
		return name(name).tags(tagKeys, tagValues).tag(key, value).build();
	}

	public static Builder name(String name) {
		return new Builder(name);
	}

	public static Builder name(String name, int estimatedTagSize) {
		return new Builder(name, estimatedTagSize);
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getTags() {
		final Map<String, String> tags = new LinkedHashMap<String, String>();
		for (int i = 0; i < tagKeys.size(); i++) {
			tags.put(tagKeys.get(i), tagValues.get(i));
		}
		return Collections.unmodifiableMap(tags);
	}

	public List<String> getTagKeys() {
		return tagKeys;
	}

	public List<String> getTagValues() {
		return tagValues;
	}

	/**
	 * Converts a metrics 2.0 name into a graphite compliant name by appending all tag values to the metric name
	 *
	 * @return A graphite compliant name
	 */
	public String toGraphiteName() {
		StringBuilder sb = new StringBuilder(GraphiteSanitizer.sanitizeGraphiteMetricSegment(name));
		for (String value : tagValues) {
			sb.append('.').append(GraphiteSanitizer.sanitizeGraphiteMetricSegment(value));
		}
		return sb.toString();
	}

	/**
	 * A {@link MetricName} is only considered equal to another {@link MetricName} if the tags are in the same order.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MetricName that = (MetricName) o;

		if (!name.equals(that.name)) return false;
		if (!tagKeys.equals(that.tagKeys)) return false;
		return tagValues.equals(that.tagValues);

	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = name.hashCode();
			result = 31 * result + tagKeys.hashCode();
			result = 31 * result + tagValues.hashCode();
			hashCode = result;
		}
		return result;
	}

	public boolean matches(MetricName other) {
		if (name.equals(other.name)) {
			return containsAllTags(other.getTags());
		} else {
			return false;
		}
	}

	private boolean containsAllTags(Map<String, String> tags) {
		for (Map.Entry<String, String> entry : tags.entrySet()) {
			if (!entry.getValue().equals(this.getTags().get(entry.getKey()))) {
				return false;
			}
		}
		return true;
	}

	public static class Builder {

		private final String name;

		private final List<String> tagKeys;
		private final List<String> tagValues;

		public Builder(String name) {
			this(name, 6);
		}

		public Builder(String name, int estimatedTagSize) {
			this.name = name;
			tagKeys = new ArrayList<String>(estimatedTagSize);
			tagValues = new ArrayList<String>(estimatedTagSize);
		}

		public Builder tag(String key, Object value) {
			this.tagKeys.add(key);
			this.tagValues.add(value.toString());
			return this;
		}

		public Builder type(String value) {
			return tag("type", value);
		}

		public Builder tier(String value) {
			return tag("tier", value);
		}

		public Builder layer(String value) {
			return tag("layer", value);
		}

		public Builder unit(String value) {
			return tag("unit", value);
		}

		public Builder tags(Map<String, String> tags) {
			this.tagKeys.addAll(tags.keySet());
			this.tagValues.addAll(tags.values());
			return this;
		}

		public Builder tags(List<String> tagKeys, List<String> tagValues) {
			this.tagKeys.addAll(tagKeys);
			this.tagValues.addAll(tagValues);
			return this;
		}

		public MetricName build() {
			return new MetricName(name, tagKeys, tagValues);
		}

	}

	@Override
	public String toString() {
		return "name='" + name + '\'' + ", tags=" + getTags();
	}

	public String getInfluxDbLineProtocolString() {
		if (influxDbLineProtocolString == null) {
			final StringBuilder sb = new StringBuilder(name.length() + tagKeys.size() * 16 + tagKeys.size());
			sb.append(escapeForInfluxDB(name));
			appendTags(sb, getTags());
			influxDbLineProtocolString = sb.toString();
		}
		return influxDbLineProtocolString;
	}

	public static String getInfluxDbTags(Map<String, String> tags) {
		final StringBuilder sb = new StringBuilder();
		appendTags(sb, tags);
		return sb.toString();
	}

	private static void appendTags(StringBuilder sb, Map<String, String> tags) {
		for (String key : new TreeSet<String>(tags.keySet())) {
			sb.append(',').append(escapeForInfluxDB(key)).append('=').append(escapeForInfluxDB(tags.get(key)));
		}
	}

	private static String escapeForInfluxDB(String s) {
		if (s.indexOf(',') != -1 || s.indexOf(' ') != -1) {
			return s.replace(" ", "\\ ").replace(",", "\\,");
		}
		return s;
	}

}
