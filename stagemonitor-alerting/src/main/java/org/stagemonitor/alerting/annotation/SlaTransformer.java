package org.stagemonitor.alerting.annotation;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.check.MetricCategory;
import org.stagemonitor.alerting.check.Threshold;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.requestmonitor.MonitorRequests;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

public class SlaTransformer extends StagemonitorByteBuddyTransformer {

	private static final Logger logger = LoggerFactory.getLogger(SlaTransformer.class);

	private static List<Check> checksCreatedBeforeMeasurementStarted = new LinkedList<Check>();

	@Override
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return isAnnotatedWith(SLA.class).or(isAnnotatedWith(SLAs.class));
	}

	@Override
	protected List<StagemonitorDynamicValue<?>> getDynamicValues() {
		return Collections.<StagemonitorDynamicValue<?>>singletonList(new MonitorSLAsDynamicValue());
	}

	/**
	 * Empty method whose sole purpose is to trigger
	 * {@link MonitorSLAsDynamicValue#resolve(MethodDescription.InDefinedShape, ParameterDescription.InDefinedShape,
	 * AnnotationDescription.Loadable, boolean)}
	 *
	 * @param dummy just a dummy value
	 */
	@Advice.OnMethodEnter
	private static void enter(@MonitorSLAs Object dummy) {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	protected @interface MonitorSLAs {
	}

	// TODO use Listener onIngore + matcher + ignore all types
	/**
	 * A fake {@link net.bytebuddy.asm.Advice.DynamicValue} implementation that does not actually inject a dynamic value
	 * but instead triggers the {@link #monitorSla(String, String, SLA, SLAs, MonitorRequests)} method to create SLA
	 * checks for the method.
	 */
	public static class MonitorSLAsDynamicValue extends StagemonitorDynamicValue<MonitorSLAs> {

		@Override
		public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
							  ParameterDescription.InDefinedShape target,
							  AnnotationDescription.Loadable<MonitorSLAs> annotation,
							  boolean initialized) {
			final String requestName = configuration.getConfig(RequestMonitorPlugin.class).getBusinessTransactionNamingStrategy()
					.getBusinessTransationName(instrumentedMethod.getDeclaringType().getSimpleName(), instrumentedMethod.getName());
			monitorSla(instrumentedMethod.toString(), requestName,
					getAnnotationOrNull(instrumentedMethod, SLA.class),
					getAnnotationOrNull(instrumentedMethod, SLAs.class),
					getAnnotationOrNull(instrumentedMethod, MonitorRequests.class));
			return null;
		}

		private <T extends Annotation> T getAnnotationOrNull(MethodDescription.InDefinedShape instrumentedMethod, Class<T> annotationClass) {
			final AnnotationDescription.Loadable<T> loadable = instrumentedMethod.getDeclaredAnnotations().ofType(annotationClass);
			if (loadable == null) {
				return null;
			}
			return loadable.loadSilent();
		}

		@Override
		public Class<MonitorSLAs> getAnnotationClass() {
			return MonitorSLAs.class;
		}
	}

	public static void monitorSla(String fullMethodSignature, String requestName, SLA slaAnnotation, SLAs slasAnnotation, MonitorRequests monitorRequests) {
		if (slaAnnotation != null || slasAnnotation != null) {
			if (monitorRequests == null) {
				logger.warn("To create an SLA for the method {}, it also has to be annotated with @MonitorRequests",
						fullMethodSignature);
			} else {
				if (slaAnnotation != null) {
					createSlaCheck(slaAnnotation, fullMethodSignature, requestName);
				}
				if (slasAnnotation != null) {
					for (SLA sla : slasAnnotation.value()) {
						createSlaCheck(sla, fullMethodSignature, requestName);
					}
				}
			}
		}
	}

	private static void createSlaCheck(SLA slaAnnotation, String fullMethodSignature, String requestName) {
		if (slaAnnotation.metric().length > 0) {
			addResponseTimeCheck(slaAnnotation, fullMethodSignature, requestName);
		}
		if (slaAnnotation.errorRateThreshold() >= 0) {
			addErrorRateCheck(slaAnnotation, fullMethodSignature, requestName);
		}
	}

	private static void addResponseTimeCheck(SLA slaAnnotation, String fullMethodSignature, String requestName) {
		SLA.Metric[] metrics = slaAnnotation.metric();
		double[] thresholdValues = slaAnnotation.threshold();
		if (metrics.length != thresholdValues.length) {
			logger.warn("The number of provided metrics don't match the number of provided thresholds in @SLA {}", fullMethodSignature);
			return;
		}

		final MetricName timerMetricName = RequestMonitor.getTimerMetricName(requestName);
		Check check = createCheck(slaAnnotation, fullMethodSignature, requestName, MetricCategory.TIMER, timerMetricName,
				" (response time)", "responseTime");

		final List<Threshold> thresholds = check.getThresholds(slaAnnotation.severity());
		for (int i = 0; i < metrics.length; i++) {
			thresholds.add(new Threshold(metrics[i].getValue(), slaAnnotation.operator(), thresholdValues[i]));
		}

		addCheckIfStarted(check);
	}

	private static void addErrorRateCheck(SLA slaAnnotation, String fullMethodSignature, String requestName) {
		final MetricName errorMetricName = RequestMonitor.getErrorMetricName(requestName);
		final Check check = createCheck(slaAnnotation, fullMethodSignature, requestName, MetricCategory.METER, errorMetricName, " (errors)", "errors");
		final Threshold t = new Threshold(SLA.Metric.M1_RATE.getValue(), Threshold.Operator.GREATER_EQUAL, slaAnnotation.errorRateThreshold());
		check.getThresholds(slaAnnotation.severity()).add(t);
		addCheckIfStarted(check);
	}

	private static Check createCheck(SLA slaAnnotation, String fullMethodSignature, String requestName, MetricCategory metricCategory,
							  MetricName metricName, String checkNameSuffix, String checkIdSuffix) {
		Check check = new Check();
		check.setId(fullMethodSignature + "." + checkIdSuffix);
		check.setName(requestName + checkNameSuffix);
		check.setMetricCategory(metricCategory);
		check.setTarget(Pattern.compile(Pattern.quote(metricName.toGraphiteName())));
		check.setAlertAfterXFailures(slaAnnotation.alertAfterXFailures());
		return check;
	}

	private static void addCheckIfStarted(Check check) {
		if (Stagemonitor.isStarted()) {
			addCheck(check);
		} else {
			checksCreatedBeforeMeasurementStarted.add(check);
		}
	}

	private static void addCheck(Check check) {
		check.setApplication(configuration.getConfig(CorePlugin.class).getMeasurementSession().getApplicationName());
		try {
			configuration.getConfig(AlertingPlugin.class).addCheck(check);
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
	}

	public static void onStart() {
		for (Check check : checksCreatedBeforeMeasurementStarted) {
			addCheck(check);
		}
		checksCreatedBeforeMeasurementStarted.clear();
	}
}
