package org.stagemonitor.tracing.sampling;

import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.RateLimiter;

import java.util.HashMap;
import java.util.Map;

public class RateLimitingPreExecutionInterceptor extends PreExecutionSpanInterceptor {

	private RateLimiter defaultSpanRateLimiter;
	private Map<String, RateLimiter> clientSpanRateLimiterByType;

	@Override
	public void init(ConfigurationRegistry configuration) {
		TracingPlugin tracingPlugin = configuration.getConfig(TracingPlugin.class);

		defaultSpanRateLimiter = getRateLimiter(tracingPlugin.getDefaultRateLimitSpansPerMinute());
		tracingPlugin.getDefaultRateLimitSpansPerMinuteOption().addChangeListener(new ConfigurationOption.ChangeListener<Double>() {
			@Override
			public void onChange(ConfigurationOption<?> configurationOption, Double oldValue, Double newValue) {
				defaultSpanRateLimiter = getRateLimiter(newValue);
			}
		});

		setRateLimiterMap(tracingPlugin.getRateLimitSpansPerMinutePerType());
		tracingPlugin.getRateLimitClientSpansPerTypePerMinuteOption().addChangeListener(new ConfigurationOption.ChangeListener<Map<String, Double>>() {
			@Override
			public void onChange(ConfigurationOption<?> configurationOption, Map<String, Double> oldValue, Map<String, Double> newValue) {
				setRateLimiterMap(newValue);
			}
		});
	}

	private void setRateLimiterMap(Map<String, Double> newValue) {
		Map<String, RateLimiter> rateLimiters = new HashMap<String, RateLimiter>();
		for (Map.Entry<String, Double> entry : newValue.entrySet()) {
			rateLimiters.put(entry.getKey(), getRateLimiter(entry.getValue()));
		}
		clientSpanRateLimiterByType = rateLimiters;
	}

	public static RateLimiter getRateLimiter(double creditsPerMinute) {
		if (creditsPerMinute >= 1000000) {
			return null;
		}
		final double maxTracesPerSecond = creditsPerMinute / 60;
		double maxBalance;
		if (maxTracesPerSecond <= 0) {
			maxBalance = 0.0;
		} else if (maxTracesPerSecond < 1.0) {
			maxBalance = 1.0;
		} else {
			maxBalance = maxTracesPerSecond;
		}
		return new RateLimiter(maxTracesPerSecond, maxBalance);
	}

	@Override
	public void interceptReport(PreExecutionInterceptorContext context) {
		final SpanContextInformation spanContext = context.getSpanContext();
		final RateLimiter rateLimiter;
		if (clientSpanRateLimiterByType.containsKey(spanContext.getOperationType())) {
			rateLimiter = clientSpanRateLimiterByType.get(spanContext.getOperationType());
		} else {
			rateLimiter = defaultSpanRateLimiter;
		}
		if (isRateExceeded(rateLimiter)) {
			context.shouldNotReport(getClass());
		}
	}

	public static boolean isRateExceeded(RateLimiter rateLimiter) {
		return rateLimiter != null && !rateLimiter.checkCredit(1.0);
	}

}
