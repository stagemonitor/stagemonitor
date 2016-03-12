package org.stagemonitor.core.metrics.metrics2;

import org.stagemonitor.core.configuration.converter.ValueConverter;

public class MetricNameValueConverter implements ValueConverter<MetricName> {
	@Override
	public MetricName convert(String s) throws IllegalArgumentException {
		return MetricName.name(s).build();
	}

	@Override
	public String toString(MetricName value) {
		return value.getName();
	}
}
