package org.stagemonitor.core.metrics.metrics2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.stagemonitor.core.util.GraphiteSanitizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

	@JsonIgnore
	private int hashCode;

	private final String name;

	// The insertion order is important for the correctness of #toGraphiteName
	private final LinkedHashMap<String, String> tags;

	private MetricName(String name, LinkedHashMap<String, String> tags) {
		this.name = name;
		this.tags = tags;
	}

	@JsonCreator
	private MetricName(@JsonProperty("name") String name, @JsonProperty("tags") Map<String, String> tags) {
		this.name = name;
		this.tags = new LinkedHashMap<String, String>(tags);
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
		return name(name).tags(tags).tag(key, value).build();
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

	public String getName() {
		return name;
	}

	public Map<String, String> getTags() {
		return Collections.unmodifiableMap(tags);
	}

	@JsonIgnore
	public List<String> getTagKeys() {
		return new ArrayList<String>(tags.keySet());
	}

	@JsonIgnore
	public List<String> getTagValues() {
		return new ArrayList<String>(tags.values());
	}

	/**
	 * Converts a metrics 2.0 name into a graphite compliant name by appending all tag values to the metric name
	 *
	 * @return A graphite compliant name
	 */
	public String toGraphiteName() {
		StringBuilder sb = new StringBuilder(GraphiteSanitizer.sanitizeGraphiteMetricSegment(name));
		for (String value : tags.values()) {
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

		return name.equals(that.name) && tags.equals(that.tags);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = name.hashCode();
			result = 31 * result + tags.hashCode();
			hashCode = result;
		}
		return result;
	}

	public boolean matches(MetricName other) {
		return name.equals(other.name) && containsAllTags(other.tags);
	}

	private boolean containsAllTags(Map<String, String> tags) {
		for (Map.Entry<String, String> entry : tags.entrySet()) {
			if (!entry.getValue().equals(this.tags.get(entry.getKey()))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * A {@link MetricNameTemplate} lets you efficiently create similar {@link MetricName}s so that if a {@link
	 * MetricName} has already been {@link #build(String)} for the same value(s), the previous instance is reused.
	 * <p/>
	 * In other words, this is a cache for {@link MetricName}s
	 * <p/>
	 * Example:
	 * <pre>
	 *     MetricName.MetricNameTemplate timerMetricNameTemplate = name("response_time_server")
	 *             .tag("request_name", "")
	 *             .layer("All")
	 *             .templateFor("request_name");
	 *     MetricName metricName = timerMetricNameTemplate.build("Search Products");
	 * </pre>
	 */
	public static class MetricNameTemplate {
		private final ConcurrentMap<Object, MetricName> metricNameCache = new ConcurrentHashMap<Object, MetricName>();
		private final MetricName template;
		private final String key;
		private final List<String> keys;

		private MetricNameTemplate(MetricName template, String key) {
			this.template = template;
			this.key = key;
			this.keys = null;
		}

		private MetricNameTemplate(MetricName template, String... keys) {
			this.template = template;
			this.key = null;
			this.keys = Arrays.asList(keys);
		}

		/**
		 * Creates a new or reused {@link MetricName} according to the {@link #template} with the given {@link #key}
		 * and the provided value
		 *
		 * @param value The tag value
		 * @return A {@link MetricName} according to the {@link #template}
		 * @throws IllegalArgumentException When this template is intended for multiple values i.e. was initialized via
		 *                                  {@link MetricNameTemplate#MetricNameTemplate(MetricName, String...)}
		 */
		public MetricName build(String value) {
			if (key == null) {
				throw new IllegalArgumentException("Size of key does not match size of values");
			}
			MetricName metricName = metricNameCache.get(value);
			if (metricName == null) {
				metricName = template.withTag(key, value);
				metricNameCache.put(value, metricName);
			}
			return metricName;
		}

		/**
		 * Creates a new or reused {@link MetricName} according to the {@link #template} with the given {@link #keys}
		 * and the provided values
		 *
		 * @param values The tag values (must match the size of {@link #keys}
		 * @return A {@link MetricName} according to the {@link #template}
		 * @throws IllegalArgumentException When number of {@link #keys} does not match the number of provided values or
		 *                                  this template is intended for a single value i.e. was initialized via {@link
		 *                                  MetricNameTemplate#MetricNameTemplate(MetricName, String)}
		 */
		public MetricName build(String... values) {
			if (keys == null || keys.size() != values.length) {
				throw new IllegalArgumentException("Size of key does not match size of values");
			}
			List<String> valuesList = Arrays.asList(values);
			MetricName metricName = metricNameCache.get(valuesList);
			if (metricName == null) {
				Builder builder = name(template.name).tags(template.tags);
				for (int i = 0; i < keys.size(); i++) {
					builder.tag(keys.get(i), valuesList.get(i));
				}
				metricName = builder.build();
				metricNameCache.put(valuesList, metricName);
			}
			return metricName;
		}
	}

	public static class Builder {

		private final String name;

		private final LinkedHashMap<String, String> tags = new LinkedHashMap<String, String>(8);

		public Builder(String name) {
			this.name = name;
		}

		/**
		 * Adds a tag to the metric name.
		 *
		 * @param key   The key should only contain alphanumerical chars and underscores.
		 * @param value The value can contain unicode characters, but it is recommended to not use white spaces.
		 * @return <code>this</code> for chaining
		 */
		public Builder tag(String key, String value) {
			this.tags.put(key, value);
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
			this.tags.putAll(tags);
			return this;
		}

		public MetricName build() {
			return new MetricName(name, tags);
		}

		/**
		 * Creates a {@link MetricNameTemplate} with a single tag
		 *
		 * @param key The template tag key
		 * @return The {@link MetricNameTemplate}
		 */
		public MetricNameTemplate templateFor(String key) {
			return new MetricNameTemplate(build(), key);
		}

		/**
		 * Creates a {@link MetricNameTemplate} with multiple tags
		 *
		 * @param keys The template tag keys
		 * @return The {@link MetricNameTemplate}
		 */
		public MetricNameTemplate templateFor(String... keys) {
			return new MetricNameTemplate(build(), keys);
		}

	}

	@Override
	public String toString() {
		return "name='" + name + '\'' + ", tags=" + getTags();
	}

}
