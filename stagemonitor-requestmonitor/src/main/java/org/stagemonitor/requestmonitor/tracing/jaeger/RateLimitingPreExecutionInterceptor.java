package org.stagemonitor.requestmonitor.tracing.jaeger;

import com.uber.jaeger.utils.RateLimiter;

import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.sampling.PreExecutionInterceptorContext;
import org.stagemonitor.requestmonitor.sampling.PreExecutionSpanInterceptor;

public class RateLimitingPreExecutionInterceptor extends PreExecutionSpanInterceptor {

	private RateLimiter serverSpanRateLimiter;
	private RateLimiter clientSpanRateLimiter;

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
	}

	private static RateLimiter getRateLimiter(double spansPerMinute) {
		if (spansPerMinute >= 1000000) {
			return null;
		}
		return new RateLimiter(spansPerMinute / 60);
	}

	@Override
	public void interceptReport(PreExecutionInterceptorContext context) {
		final SpanContextInformation spanContext = context.getSpanContext();
		boolean rateExceeded = false;
		if (spanContext.isServerRequest() && serverSpanRateLimiter != null) {
			rateExceeded = !serverSpanRateLimiter.checkCredit(1.0);
		} else if (spanContext.isExternalRequest() && clientSpanRateLimiter != null) {
			rateExceeded = !clientSpanRateLimiter.checkCredit(1.0);
		}
		if (rateExceeded) {
			context.shouldNotReport(getClass());
		}
	}

}
