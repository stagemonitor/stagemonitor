package org.stagemonitor.alerting.annotation;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
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
import org.stagemonitor.requestmonitor.AbstractMonitorRequestsTransformer;
import org.stagemonitor.requestmonitor.MonitorRequests;
import org.stagemonitor.requestmonitor.RequestMonitor;

public class SlaTransformer extends StagemonitorByteBuddyTransformer {

	private static final Logger logger = LoggerFactory.getLogger(SlaTransformer.class);

	private static List<Check> checksCreatedBeforeMeasurementStarted = new LinkedList<Check>();

	@Override
	protected ElementMatcher.Junction<TypeDescription> getIncludeTypeMatcher() {
		return super.getIncludeTypeMatcher()
				.and(isAnnotatedWith(SLA.class).or(isAnnotatedWith(SLAs.class)));
	}

	@Override
	protected ElementMatcher.Junction<? super MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return isAnnotatedWith(SLA.class).or(isAnnotatedWith(SLAs.class));
	}

	@Advice.OnMethodEnter
	private static void enter(@Advice.Origin String fullMethodSignature,
							  @AbstractMonitorRequestsTransformer.RequestName String requestName,
							  @InjectAnnotation(SLA.class) SLA slaAnnotation,
							  @InjectAnnotation(SLAs.class) SLAs slasAnnotation,
							  @InjectAnnotation(MonitorRequests.class) MonitorRequests monitorRequests) {
		monitorSla(fullMethodSignature, requestName, slaAnnotation, slasAnnotation, monitorRequests);
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