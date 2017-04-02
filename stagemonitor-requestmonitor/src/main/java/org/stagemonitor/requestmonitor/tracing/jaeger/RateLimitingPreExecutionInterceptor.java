package org.stagemonitor.requestmonitor.tracing.jaeger;

import com.uber.jaeger.utils.RateLimiter;

import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.sampling.PreExecutionInterceptorContext;
import org.stagemonitor.requestmonitor.sampling.PreExecutionSpanInterceptor;

import java.util.HashMap;
import java.util.Map;

public class RateLimitingPreExecutionInterceptor extends PreExecutionSpanInterceptor {

	private RateLimiter serverSpanRateLimiter;
	private RateLimiter clientSpanRateLimiter;
	private Map<String, RateLimiter> clientSpanRateLimiterByType;

	@Override
	public void init(Configuration configuration) {
		RequestMonitorPlugin requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
		serverSpanRateLimiter = getRateLimiter(requestMonitorPlugin.getRateLimitServerSpansPerMinute());
		requestMonitorPlugin.getRateLimitServerSpansPerMinuteOption().addChangeListener(new ConfigurationOption.ChangeListener<Double>() {
			@Override
			public void onChange(ConfigurationOption<?> configurationOption, Double oldValue, Double newValue) {
				serverSpanRateLimiter = getRateLimiter(newValue);
			}
		});

		clientSpanRateLimiter = getRateLimiter(requestMonitorPlugin.getRateLimitClientSpansPerMinute());
		requestMonitorPlugin.getRateLimitClientSpansPerMinuteOption().addChangeListener(new ConfigurationOption.ChangeListener<Double>() {
			@Override
			public void onChange(ConfigurationOption<?> configurationOption, Double oldValue, Double newValue) {
				clientSpanRateLimiter = getRateLimiter(newValue);
			}
		});

		setRateLimiterMap(requestMonitorPlugin.getRateLimitClientSpansPerTypePerMinute());
		requestMonitorPlugin.getRateLimitClientSpansPerTypePerMinuteOption().addChangeListener(new ConfigurationOption.ChangeListener<Map<String, Double>>() {
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
		return new RateLimiter(creditsPerMinute / 60);
	}

	@Override
	public void interceptReport(PreExecutionInterceptorContext context) {
		final SpanContextInformation spanContext = context.getSpanContext();
		boolean rateExceeded = false;
		if (spanContext.isServerRequest()) {
			rateExceeded = isRateExceeded(serverSpanRateLimiter);
		} else if (spanContext.isExternalRequest()) {
			RateLimiter rateLimiter = clientSpanRateLimiter;
			if (clientSpanRateLimiterByType.containsKey(spanContext.getOperationType())) {
				rateLimiter = clientSpanRateLimiterByType.get(spanContext.getOperationType());
			}
			rateExceeded = isRateExceeded(rateLimiter);
		}
		if (rateExceeded) {
			context.shouldNotReport(getClass());
		}
	}

	private boolean isRateExceeded(RateLimiter rateLimiter) {
		return rateLimiter != null && !rateLimiter.checkCredit(1.0);
	}

}
