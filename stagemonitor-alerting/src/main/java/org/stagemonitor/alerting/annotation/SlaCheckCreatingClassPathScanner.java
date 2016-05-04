package org.stagemonitor.alerting.annotation;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.check.MetricCategory;
import org.stagemonitor.alerting.check.Threshold;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.instrument.AbstractClassPathScanner;
import org.stagemonitor.core.metrics.annotations.ExceptionMeteredTransformer;
import org.stagemonitor.core.metrics.annotations.TimedTransformer;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.requestmonitor.AbstractMonitorRequestsTransformer;
import org.stagemonitor.requestmonitor.MonitorRequests;
import org.stagemonitor.requestmonitor.RequestMonitor;

public class SlaCheckCreatingClassPathScanner extends AbstractClassPathScanner {

	private static final Logger logger = LoggerFactory.getLogger(SlaCheckCreatingClassPathScanner.class);

	private static final Object measurementSessionLock = new Object();
	private static List<Check> checksCreatedBeforeMeasurementStarted = new LinkedList<Check>();
	private static volatile MeasurementSession measurementSession;

	@Override
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return isAnnotatedWith(SLA.class).or(isAnnotatedWith(SLAs.class));
	}

	@Override
	protected void onMethodMatch(MethodDescription.InDefinedShape methodDescription) {
		final String fullMethodSignature = methodDescription.toString();
		final AnnotationList methodAnnotations = methodDescription.getDeclaredAnnotations();

		TimerNames timerNames = getTimerNames(methodDescription, methodAnnotations);
		if (timerNames.hasAnyName()) {
			createChecks(fullMethodSignature, methodAnnotations, timerNames);
		} else {
			timerNames = getTimerNames(methodDescription, methodDescription.getDeclaringType().getInheritedAnnotations());
			createChecks(fullMethodSignature, methodAnnotations, timerNames);
		}
	}

	private static class TimerNames {
		private MetricName timerMetricName;
		private String timerName;
		private String errorRequestName;
		private MetricName errorMetricName;

		boolean hasAnyName() {
			return timerName != null || errorRequestName != null;
		}
	}

	private TimerNames getTimerNames(MethodDescription.InDefinedShape methodDescription, AnnotationList declaredAnnotations) {
		TimerNames timerNames = new TimerNames();
		if (declaredAnnotations.isAnnotationPresent(MonitorRequests.class)) {
			timerNames.timerName = AbstractMonitorRequestsTransformer.getRequestName(methodDescription);
			if (timerNames.timerName != null) {
				timerNames.timerMetricName = RequestMonitor.getTimerMetricName(timerNames.timerName);
				timerNames.errorRequestName = timerNames.timerName;
				timerNames.errorMetricName = RequestMonitor.getErrorMetricName(timerNames.timerName);
			}
		} else {
			if (declaredAnnotations.isAnnotationPresent(Timed.class)) {
				timerNames.timerName = new TimedTransformer.TimedSignatureDynamicValue().getRequestName(methodDescription);
				timerNames.timerMetricName = TimedTransformer.getTimerName(timerNames.timerName);
			}
			if (declaredAnnotations.isAnnotationPresent(ExceptionMetered.class)) {
				timerNames.errorRequestName = new ExceptionMeteredTransformer.ExceptionMeteredSignatureDynamicValue().getRequestName(methodDescription);
				timerNames.errorMetricName = ExceptionMeteredTransformer.getMetricName(timerNames.errorRequestName);
			}
		}
		return timerNames;
	}

	private void createChecks(String fullMethodSignature, AnnotationList declaredAnnotations, TimerNames timerNames) {
		if (declaredAnnotations.isAnnotationPresent(SLA.class)) {
			createSlaCheck(declaredAnnotations.ofType(SLA.class).loadSilent(), fullMethodSignature, timerNames);
		}
		if (declaredAnnotations.isAnnotationPresent(SLAs.class)) {
			for (SLA sla : declaredAnnotations.ofType(SLAs.class).loadSilent().value()) {
				createSlaCheck(sla, fullMethodSignature, timerNames);
			}
		}
	}

	private static void createSlaCheck(SLA slaAnnotation, String fullMethodSignature, TimerNames timerNames) {
		if (slaAnnotation.metric().length > 0) {
			addResponseTimeCheck(slaAnnotation, fullMethodSignature, timerNames);
		}
		if (slaAnnotation.errorRateThreshold() >= 0) {
			addErrorRateCheck(slaAnnotation, fullMethodSignature, timerNames);
		}
	}

	private static void addResponseTimeCheck(SLA slaAnnotation, String fullMethodSignature, TimerNames timerNames) {
		SLA.Metric[] metrics = slaAnnotation.metric();
		double[] thresholdValues = slaAnnotation.threshold();
		if (metrics.length != thresholdValues.length) {
			logger.warn("The number of provided metrics don't match the number of provided thresholds in @SLA {}", fullMethodSignature);
			return;
		}
		if (timerNames.timerName == null) {
			logger.warn("To create a timer SLA for the method {}, it also has to be annotated with @MonitorRequests or " +
					" @Timed. When using @MonitorRequests, resolveNameAtRuntime must not be set to true.", fullMethodSignature);
			return;
		}

		Check check = createCheck(slaAnnotation, fullMethodSignature, timerNames.timerName, MetricCategory.TIMER,
				timerNames.timerMetricName, " (response time)", "responseTime");

		final List<Threshold> thresholds = check.getThresholds(slaAnnotation.severity());
		for (int i = 0; i < metrics.length; i++) {
			thresholds.add(new Threshold(metrics[i].getValue(), slaAnnotation.operator(), thresholdValues[i]));
		}

		addCheckIfStarted(check);
	}

	private static void addErrorRateCheck(SLA slaAnnotation, String fullMethodSignature, TimerNames timerNames) {
		if (timerNames.errorRequestName == null) {
			logger.warn("To create an error SLA for the method {}, it also has to be annotated with @MonitorRequests or " +
					" @ExceptionMetered. When using @MonitorRequests, resolveNameAtRuntime must not be set to true.", fullMethodSignature);
			return;
		}
		final Check check = createCheck(slaAnnotation, fullMethodSignature, timerNames.errorRequestName, MetricCategory.METER, timerNames.errorMetricName, " (errors)", "errors");
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
		synchronized (measurementSessionLock) {
			if (measurementSession != null) {
				addCheck(check, measurementSession);
			} else {
				checksCreatedBeforeMeasurementStarted.add(check);
			}
		}
	}

	private static void addCheck(Check check, MeasurementSession measurementSession) {
		check.setApplication(measurementSession.getApplicationName());
		try {
			configuration.getConfig(AlertingPlugin.class).addCheck(check);
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
	}

	public static void onStart(MeasurementSession measurementSession) {
		synchronized (measurementSessionLock) {
			System.out.println("onStart");
			SlaCheckCreatingClassPathScanner.measurementSession = measurementSession;
			for (Check check : checksCreatedBeforeMeasurementStarted) {
				addCheck(check, measurementSession);
			}
			checksCreatedBeforeMeasurementStarted.clear();
		}
	}
}
