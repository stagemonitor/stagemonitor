package org.stagemonitor.requestmonitor.sampling;

import com.uber.jaeger.utils.RateLimiter;

import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

class RateLimitingPreExecutionInterceptor extends PreExecutionSpanReporterInterceptor {

	private RateLimiter serverSpanRateLimiter;
	private RateLimiter clientSpanRateLimiter;

	public RateLimitingPreExecutionInterceptor(RequestMonitorPlugin requestMonitorPlugin) {
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
		final RequestMonitor.RequestInformation requestInformation = context.getRequestInformation();
		boolean rateExceeded = false;
		if (requestInformation.isServerRequest() && serverSpanRateLimiter != null) {
			rateExceeded = !serverSpanRateLimiter.checkCredit(1.0);
		} else if (requestInformation.isExternalRequest() && clientSpanRateLimiter != null) {
			rateExceeded = !clientSpanRateLimiter.checkCredit(1.0);
		}
		if (rateExceeded) {
			context.shouldNotReport(getClass());
		}
	}

}
