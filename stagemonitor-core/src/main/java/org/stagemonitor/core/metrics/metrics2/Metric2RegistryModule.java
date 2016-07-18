package org.stagemonitor.core.metrics.metrics2;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Metric2RegistryModule extends Module {

	private final double rateFactor;
	private final double durationFactor;
	private final Metric2Filter filter;

	public Metric2RegistryModule(TimeUnit rateUnit, TimeUnit durationUnit) {
		this(rateUnit, durationUnit, Metric2Filter.ALL);
	}

	public Metric2RegistryModule(TimeUnit rateUnit, TimeUnit durationUnit, Metric2Filter filter) {
		this.rateFactor = rateUnit.toSeconds(1);
		this.durationFactor = 1.0 / durationUnit.toNanos(1);
		this.filter = filter;
	}

	@Override
	public String getModuleName() {
		return "stagemonitor";
	}

	@Override
	public Version version() {
		return new Version(1, 0, 0, "", "org.stagemonitor", "stagemonitor-core");
	}

	@Override
	public void setupModule(SetupContext context) {
		context.addSerializers(new SimpleSerializers(Collections.<JsonSerializer<?>>singletonList(new Metric2RegistrySerializer())));
	}

	private class Metric2RegistrySerializer extends StdSerializer<Metric2Registry> {
		public Metric2RegistrySerializer() {
			super(Metric2Registry.class);
		}

		@Override
		public void serialize(Metric2Registry value, JsonGenerator gen, SerializerProvider provider) throws IOException {
			gen.writeStartArray();
			new MetricSerializer<Gauge>(Gauge.class, new GaugeValueWriter()).serialize(value.getGauges(filter), gen, provider);
			new MetricSerializer<Counter>(Counter.class, new CounterValueWriter()).serialize(value.getCounters(filter), gen, provider);
			new MetricSerializer<Histogram>(Histogram.class, new HistogramValueWriter()).serialize(value.getHistograms(filter), gen, provider);
			new MetricSerializer<Meter>(Meter.class, new MeterValueWriter()).serialize(value.getMeters(filter), gen, provider);
			new MetricSerializer<Timer>(Timer.class, new TimerValueWriter()).serialize(value.getTimers(filter), gen, provider);
			gen.writeEndArray();
		}
	}

	private class MetricSerializer<T extends Metric> extends StdSerializer<Map<MetricName, T>> {
		private final ValueWriter<T> valueWriter;

		public MetricSerializer(Class<T> metricType, ValueWriter<T> valueWriter) {
			super(TypeFactory.defaultInstance().constructMapType(Map.class, MetricName.class, metricType));
			this.valueWriter = valueWriter;
		}

		@Override
		public void serialize(Map<MetricName, T> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
			for (Map.Entry<MetricName, T> entry : value.entrySet()) {
				gen.writeStartObject();
				MetricName metricName = entry.getKey();
				gen.writeStringField("name", metricName.getName());
				gen.writeObjectFieldStart("tags");
				for (Map.Entry<String, String> tagEntry : metricName.getTags().entrySet()) {
					gen.writeObjectField(tagEntry.getKey(), tagEntry.getValue());
				}
				gen.writeEndObject();
				gen.writeObjectFieldStart("values");
				valueWriter.writeValues(entry.getValue(), gen);
				gen.writeEndObject();
				gen.writeEndObject();
			}
		}
	}

	public <T extends Metric> ValueWriter<T> getValueWriter(Class<T> metricClass) {
		if (Gauge.class == metricClass) {
			return (ValueWriter<T>) new GaugeValueWriter();
		} else if (Counter.class == metricClass) {
			return (ValueWriter<T>) new CounterValueWriter();
		} else if (Histogram.class == metricClass) {
			return (ValueWriter<T>) new HistogramValueWriter();
		} else if (Meter.class == metricClass) {
			return (ValueWriter<T>) new MeterValueWriter();
		} else if (Timer.class == metricClass) {
			return (ValueWriter<T>) new TimerValueWriter();
		} else {
			throw new IllegalArgumentException("Unknown metric class: " + metricClass);
		}
	}

	public interface ValueWriter<T extends Metric> {
		void writeValues(T value, JsonGenerator jg) throws IOException;
	}

	private class GaugeValueWriter implements ValueWriter<Gauge> {
		public void writeValues(Gauge gauge, JsonGenerator jg) throws IOException {
			final Object value = gauge.getValue();
			if (value == null) {
				return;
			}
			if (value instanceof Number) {
				writeDoubleUnlessNaN(jg, "value", ((Number) value).doubleValue());
			} else if (value instanceof Boolean) {
				jg.writeBooleanField("value_boolean", (Boolean) value);
			} else {
				jg.writeStringField("value_string", value.toString());
			}
		}
	}

	private class CounterValueWriter implements ValueWriter<Counter> {
		public void writeValues(Counter counter, JsonGenerator jg) throws IOException {
			jg.writeObjectField("count", counter.getCount());
		}
	}

	private class HistogramValueWriter implements ValueWriter<Histogram> {
		public void writeValues(Histogram histogram, JsonGenerator jg) throws IOException {
			final Snapshot snapshot = histogram.getSnapshot();
			jg.writeNumberField("count", histogram.getCount());
			writeSnapshot(snapshot, jg);
		}
	}

	private class MeterValueWriter implements ValueWriter<Meter> {
		public void writeValues(Meter meter, JsonGenerator jg) throws IOException {
			writeMetered(meter, jg);
		}
	}

	private class TimerValueWriter implements ValueWriter<Timer> {
		public void writeValues(Timer timer, JsonGenerator jg) throws IOException {
			writeMetered(timer, jg);
			writeSnapshot(timer.getSnapshot(), jg);
		}
	}

	private void writeSnapshot(Snapshot snapshot, JsonGenerator jg) throws IOException {
		writeDoubleUnlessNaN(jg, "min", convertDuration(snapshot.getMin()));
		writeDoubleUnlessNaN(jg, "max", convertDuration(snapshot.getMax()));
		writeDoubleUnlessNaN(jg, "mean", convertDuration(snapshot.getMean()));
		writeDoubleUnlessNaN(jg, "std", convertDuration(snapshot.getStdDev()));
		writeDoubleUnlessNaN(jg, "p25", convertDuration(snapshot.getValue(0.25)));
		writeDoubleUnlessNaN(jg, "p50", convertDuration(snapshot.getMedian()));
		writeDoubleUnlessNaN(jg, "p75", convertDuration(snapshot.get75thPercentile()));
		writeDoubleUnlessNaN(jg, "p95", convertDuration(snapshot.get95thPercentile()));
		writeDoubleUnlessNaN(jg, "p98", convertDuration(snapshot.get98thPercentile()));
		writeDoubleUnlessNaN(jg, "p99", convertDuration(snapshot.get99thPercentile()));
		writeDoubleUnlessNaN(jg, "p999", convertDuration(snapshot.get999thPercentile()));
	}

	private void writeMetered(Metered metered, JsonGenerator jg) throws IOException {
		jg.writeNumberField("count", metered.getCount());
		writeDoubleUnlessNaN(jg, "m1_rate", convertRate(metered.getOneMinuteRate()));
		writeDoubleUnlessNaN(jg, "m5_rate", convertRate(metered.getFiveMinuteRate()));
		writeDoubleUnlessNaN(jg, "m15_rate", convertRate(metered.getFifteenMinuteRate()));
		writeDoubleUnlessNaN(jg, "mean_rate", convertRate(metered.getMeanRate()));
	}

	private void writeDoubleUnlessNaN(JsonGenerator jg, String key, double value) throws IOException {
		if (!Double.isNaN(value)) {
			jg.writeNumberField(key, value);
		}
	}


	private double convertDuration(double duration) {
		return duration * durationFactor;
	}

	private double convertRate(double rate) {
		return rate * rateFactor;
	}
}
