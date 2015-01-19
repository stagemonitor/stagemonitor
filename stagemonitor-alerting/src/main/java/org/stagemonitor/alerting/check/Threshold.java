package org.stagemonitor.alerting.check;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a threshold to check
 */
public class Threshold {

	private final Operator operator;
	private final double thresholdValue;

	@JsonCreator
	public Threshold(@JsonProperty("operator") Operator operator, @JsonProperty("thresholdValue") double thresholdValue) {
		if (operator == null) {
			throw new IllegalArgumentException("Operator may not be null");
		}
		this.operator = operator;
		this.thresholdValue = thresholdValue;
	}

	/**
	 * Checks if the threshold is exceeded.
	 *
	 * @param actualValue the value to check
	 * @return <code>true</code>, if the actual value exceeds the threshold, <code>false</code> otherwise
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

	public String toString() {
		return operator.operatorString + " " + thresholdValue;
	}

	/**
	 * Represents a boolean operator that can be used to check whether the expression
	 * <code>actualValue OPERATOR thresholdValue</code> is true or false
	 */
	public static enum Operator {

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
		 * Checks, whether <code>actualValue OPERATOR thresholdValue</code> is true or false
		 *
		 * @param actualValue
		 * @param expectedValue
		 * @return
		 */
		public abstract boolean check(double actualValue, double expectedValue);

		/**
		 * Gets a operator by its {@link #operatorString}.
		 *
		 * @param operatorString the operator string
		 * @return the operator (e.g. {@link #GREATER_EQUAL} for the operatorString <code>>=</code>)
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
