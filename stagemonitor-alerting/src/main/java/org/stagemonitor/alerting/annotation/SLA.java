package org.stagemonitor.alerting.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.alerting.check.ValueType;
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
	ValueType[] metric() default {};

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

}
