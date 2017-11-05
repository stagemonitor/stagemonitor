package org.stagemonitor.tracing.sampling;

import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.RateLimiter;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

public class RateLimitingPreExecutionInterceptor extends PreExecutionSpanInterceptor {

	private RateLimiter rateLimiter;
	private TracingPlugin tracingPlugin;

	@Override
	public void init(ConfigurationRegistry configuration) {
		tracingPlugin = configuration.getConfig(TracingPlugin.class);

		rateLimiter = getRateLimiter(tracingPlugin.getDefaultRateLimitSpansPerMinute());
		tracingPlugin.getDefaultRateLimitSpansPerMinuteOption().addChangeListener(new ConfigurationOption.ChangeListener<Double>() {
			@Override
			public void onChange(ConfigurationOption<?> configurationOption, Double oldValue, Double newValue) {
				rateLimiter = getRateLimiter(newValue);
			}
		});
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
		if (rateLimiter == null) {
			return;
		}
		// the rate limit is per span but we either sample a trace as a whole or don't sample it
		// even though we don't make sampling decisions for non root spans
		// we need to tell the rate limiter that there is another span being sampled
		// so that the cost is incorporated
		if (isRateExceeded(rateLimiter) && isRoot(context.getSpanContext().getSpanWrapper())) {
			context.shouldNotReport(getClass());
		}
	}

	protected boolean isRoot(SpanWrapper span) {
		return tracingPlugin.isRoot(span);
	}

	public static boolean isRateExceeded(RateLimiter rateLimiter) {
		return rateLimiter != null && !rateLimiter.checkCredit(1.0);
	}

}
