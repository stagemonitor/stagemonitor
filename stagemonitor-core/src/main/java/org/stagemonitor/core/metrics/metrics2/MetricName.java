package org.stagemonitor.core.metrics.metrics2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.stagemonitor.core.util.GraphiteSanitizer;

/**
 * Represents a metrics 2.0 name that consists of a name and arbitrary tags (a set of key-value-pairs).
 * <p/>
 * To create a new {@link MetricName}, use the static {@link MetricName}.{@link #name(String)} method. Example:
 * <code>name("api_request_duration").tag("stage", "transform").build()</code>
 * <p/>
 * This is needed for example for InfluxDB's data model (see https://influxdb.com/docs/v0.9/concepts/schema_and_data_layout.html)
 * and to store metrics into Elasticsearch (see https://www.elastic.co/blog/elasticsearch-as-a-time-series-data-store).
 * See also http://metrics20.org/
 * <p/>
 * The cool thing is that it is completely backwards compatible to graphite metric names and can also automatically
 * replace characters disallowed in graphite (see {@link #toGraphiteName()}).
 * <p/>
 * This class is immutable
 */
public class MetricName {

	private int hashCode;

	private final String name;

	private final List<String> tagKeys;
	private final List<String> tagValues;

	private MetricName(String name, List<String> tagKeys, List<String> tagValues) {
		this.name = name;
		this.tagKeys = Collections.unmodifiableList(tagKeys);
		this.tagValues = Collections.unmodifiableList(tagValues);
	}

	/**
	 * Returns a copy of this name and appends a single tag
	 * <p/>
	 * Note that this method does not override existing tags
	 *
	 * @param key   the key of the tag
	 * @param value the value of the tag
	 * @return a copy of this name including the provided tag
	 */
	public MetricName withTag(String key, String value) {
		return name(name).tags(tagKeys, tagValues).tag(key, value).build();
	}

	/**
	 * Constructs a new {@link Builder} with the provided name.
	 * <p/>
	 * After adding tags with {@link Builder#tag(String, Object)}, call {@link Builder#build()} to get the
	 * immutable {@link MetricName}
	 * <p/>
	 * The metric name should only contain alphanumerical chars and underscores.
	 * <p/>
	 * When in doubt how to name a metic, take a look at https://prometheus.io/docs/practices/naming/
	 *
	 * @param name the metric name
	 * @return a {@link Builder} with the provided name
	 */
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

		/**
		 * Adds a tag to the metric name.
		 *
		 * @param key The key should only contain alphanumerical chars and underscores.
		 * @param value The value can contain unicode characters, but it is recommended to not use white spaces.
		 * @return <code>this</code> for chaining
		 */
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

}
