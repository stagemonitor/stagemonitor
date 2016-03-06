package org.stagemonitor.alerting.annotation;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.check.MetricCategory;
import org.stagemonitor.alerting.check.Threshold;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.requestmonitor.MonitorRequests;
import org.stagemonitor.requestmonitor.MonitorRequestsInstrumenter;
import org.stagemonitor.requestmonitor.RequestMonitor;

public class SlaInstrumenter extends StagemonitorJavassistInstrumenter {

	private static final Logger logger = LoggerFactory.getLogger(SlaInstrumenter.class);

	private static List<Check> checksCreatedBeforeMeasurementStarted = new LinkedList<Check>();

	@Override
	public void transformClass(CtClass ctClass, ClassLoader loader) throws Exception {
		for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
			SLA slaAnnotation = (SLA) ctMethod.getAnnotation(SLA.class);
			SLAs slasAnnotation = (SLAs) ctMethod.getAnnotation(SLAs.class);
			if (slaAnnotation != null || slasAnnotation != null) {
				MonitorRequests monitorRequests = (MonitorRequests) ctMethod.getAnnotation(MonitorRequests.class);
				if (monitorRequests == null) {
					logger.warn("To create an SLA for the method {}.{}, it also has to be annotated with @MonitorRequests",
							ctClass.getSimpleName(), ctMethod.getName());
				} else {
					if (slaAnnotation != null) {
						createSlaCheck(slaAnnotation, ctClass, ctMethod);
					}
					if (slasAnnotation != null) {
						for (SLA sla : slasAnnotation.value()) {
							createSlaCheck(sla, ctClass, ctMethod);
						}
					}
				}
			}
		}
	}

	private void createSlaCheck(SLA slaAnnotation, CtClass ctClass, CtMethod ctMethod) {
		final int modifiers = ctMethod.getModifiers();
		if (Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers) || Modifier.isInterface(modifiers) || ctClass != ctMethod.getDeclaringClass()) {
			return;
		}
		final String requestName = MonitorRequestsInstrumenter.getRequestName(ctMethod);

		if (slaAnnotation.metric().length > 0) {
			addResponseTimeCheck(slaAnnotation, ctClass, ctMethod, requestName);
		}
		if (slaAnnotation.errorRateThreshold() >= 0) {
			addErrorRateCheck(slaAnnotation, requestName);
		}
	}

	private void addResponseTimeCheck(SLA slaAnnotation, CtClass ctClass, CtMethod ctMethod, String requestName) {
		SLA.Metric[] metrics = slaAnnotation.metric();
		double[] thresholdValues = slaAnnotation.threshold();
		if (metrics.length != thresholdValues.length) {
			logger.warn("The number of provided metrics don't match the number of provided thresholds in @SLA {}.{}",
					ctClass.getSimpleName(), ctMethod.getName());
			return;
		}

		Check check = createCheck(slaAnnotation, MetricCategory.TIMER, RequestMonitor.getTimerMetricName(requestName),
				requestName + " (response time)");

		final List<Threshold> thresholds = check.getThresholds(slaAnnotation.severity());
		for (int i = 0; i < metrics.length; i++) {
			thresholds.add(new Threshold(metrics[i].getValue(), slaAnnotation.operator(), thresholdValues[i]));
		}

		addCheckIfStarted(check);
	}

	private void addErrorRateCheck(SLA slaAnnotation, String requestName) {
		final Check check = createCheck(slaAnnotation, MetricCategory.METER, RequestMonitor.getErrorMetricName(requestName), requestName + " (errors)");
		final Threshold t = new Threshold(SLA.Metric.M1_RATE.getValue(), Threshold.Operator.GREATER_EQUAL, slaAnnotation.errorRateThreshold());
		check.getThresholds(slaAnnotation.severity()).add(t);
		addCheckIfStarted(check);
	}

	private Check createCheck(SLA slaAnnotation, MetricCategory metricCategory, MetricName metricName, String id) {
		Check check = new Check();
		check.setId(id);
		check.setName(check.getId());
		check.setMetricCategory(metricCategory);
		check.setTarget(Pattern.compile(Pattern.quote(metricName.toGraphiteName())));
		check.setAlertAfterXFailures(slaAnnotation.alertAfterXFailures());
		return check;
	}

	private void addCheckIfStarted(Check check) {
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
