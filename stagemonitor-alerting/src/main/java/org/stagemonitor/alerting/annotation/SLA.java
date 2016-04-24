package org.stagemonitor.alerting.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.alerting.check.Threshold;
import org.stagemonitor.requestmonitor.MonitorRequests;

/**
 * This annotation lets you define service level agreements within the code.
 * <p/>
 * It automatically creates checks for the annotated method.
 * The method also has to be annotated either with @{@link org.stagemonitor.requestmonitor.MonitorRequests}
 * ({@link MonitorRequests#resolveNameAtRuntime()} must be set to <code>false</code> then) or
 * with @{@link com.codahale.metrics.annotation.Timed} for response time SLAs
 * and @{@link com.codahale.metrics.annotation.ExceptionMetered} for {@link #errorRateThreshold()}s.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SLA {

	/**
	 * If set, a check for the error rate (errors/second) will be created
	 */
	double errorRateThreshold() default -1;

	/**
	 * The metrics a threshold should be created for
	 * <p/>
	 * Make sure the number of metrics and thresholds match
	 */
	Metric[] metric() default {};

	/**
	 * The thresholds for the metrics
	 * <p/>
	 * Make sure the number of metrics and thresholds match.
	 * Rates are per second and durations are in milliseconds.
	 */
	double[] threshold() default {};

	CheckResult.Status severity() default CheckResult.Status.ERROR;

	/**
	 * Trigger alert when <code>actualValue OPERATOR threshold</code>
	 */
	Threshold.Operator operator() default Threshold.Operator.GREATER_EQUAL;

	/**
	 * Can be used to make alerts less noisy
	 */
	int alertAfterXFailures() default 1;

	enum Metric {

		COUNT("count"), MEAN("mean"), MIN("min"), MAX("max"), STDDEV("stddev"),
		P50("p50"), P75("p75"), P95("p95"), P98("p98"), P99("p99"), P999("p999"),
		MEAN_RATE("mean_rate"), M1_RATE("m1_rate"), M5_RATE("m5_rate"), M15_RATE("m15_rate");

		private final String value;

		Metric(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}
}
