package org.stagemonitor.alerting.check;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.stagemonitor.core.metrics.metrics2.InfluxDbReporter;
import org.stagemonitor.core.metrics.metrics2.MetricName;

import java.util.Map;

/**
 * Represents a threshold to check
 */
public class Threshold {

	private final MetricValueType valueType;
	private final Operator operator;
	private final double thresholdValue;

	@JsonCreator
	public Threshold(@JsonProperty("valueType") String metric, @JsonProperty("operator") Operator operator, @JsonProperty("thresholdValue") double thresholdValue) {
		if (operator == null) {
			throw new IllegalArgumentException("Operator may not be null");
		}
		this.valueType = MetricValueType.valueOf(metric.toUpperCase());
		this.operator = operator;
		this.thresholdValue = thresholdValue;
	}

	/**
	 * Checks if the threshold is exceeded.
	 *
	 * @param actualValue the value to check
	 * @return {@code true}, if the actual value exceeds the threshold, {@code false} otherwise
	 */
	public boolean isExceeded(double actualValue) {
		return operator.check(actualValue, thresholdValue);
	}

	public Operator getOperator() {
		return operator;
	}

	public double getThresholdValue() {
		return thresholdValue;
	}

	public MetricValueType getValueType() {
		return valueType;
	}

	public String toString() {
		return valueType.getName() + " " + operator.operatorString + " " + thresholdValue;
	}

	public String getCheckExpressionAsString(MetricName target) {
		return InfluxDbReporter.getInfluxDbLineProtocolString(target) + ' ' + valueType.getName() + " " + operator.operatorString + " " + thresholdValue;
	}

	public CheckResult check(CheckResult.Status severity, MetricName target, Map<String, Number> currentValuesByMetric) {
		double actualValue = currentValuesByMetric.get(valueType.getName()).doubleValue();
		if (isExceeded(actualValue)) {
			return new CheckResult(getCheckExpressionAsString(target) + " is true", actualValue, severity);
		}
		return new CheckResult(null, actualValue, CheckResult.Status.OK);
	}

	/**
	 * Represents a boolean operator that can be used to check whether the expression
	 * {@code actualValue OPERATOR thresholdValue} is true or false
	 */
	public enum Operator {

		LESS("<") {
			@Override
			public boolean check(double actualValue, double expectedValue) {
				return actualValue < expectedValue;
			}
		},
		LESS_EQUAL("<=") {
			@Override
			public boolean check(double actualValue, double expectedValue) {
				return actualValue <= expectedValue;
			}
		},
		GREATER(">") {
			@Override
			public boolean check(double actualValue, double expectedValue) {
				return actualValue > expectedValue;
			}
		},
		GREATER_EQUAL(">=") {
			@Override
			public boolean check(double actualValue, double expectedValue) {
				return actualValue >= expectedValue;
			}
		};

		private final String operatorString;

		Operator(String operatorString) {
			this.operatorString = operatorString;
		}

		/**
		 * Checks, whether {@code actualValue OPERATOR thresholdValue} is true or false
		 *
		 * @param actualValue x
		 * @param expectedValue x
		 * @return x
		 */
		public abstract boolean check(double actualValue, double expectedValue);

		/**
		 * Gets a operator by its {@link #operatorString}.
		 *
		 * @param operatorString the operator string
		 * @return the operator (e.g. {@link #GREATER_EQUAL} for the operatorString {@code >=})
		 */
		public static Operator getByString(String operatorString) {
			for (Operator operator : Operator.values()) {
				if (operator.operatorString.equals(operatorString)) {
					return operator;
				}
			}
			throw new IllegalArgumentException("Operator '" + operatorString + "' does not exist.");
		}

	}
}
