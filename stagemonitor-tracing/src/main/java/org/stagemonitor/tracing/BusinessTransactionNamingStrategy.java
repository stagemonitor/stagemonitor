package org.stagemonitor.tracing;

import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.util.StringUtils;

/**
 * Determines how a business transaction should be named
 */
public enum BusinessTransactionNamingStrategy {

	METHOD_NAME_SPLIT_CAMEL_CASE {
		@Override
		public String getBusinessTransactionName(String fullClassName, String methodName) {
			return StringUtils.capitalize(StringUtils.splitCamelCase(methodName));
		}

		@Override
		public String toString() {
			return "Split method name by camel case (e.g. Method Name)";
		}
	},
	CLASS_NAME_HASH_METHOD_NAME {
		@Override
		public String getBusinessTransactionName(String fullClassName, String methodName) {
			return ClassUtils.shorten(fullClassName) + "#" + methodName;
		}

		@Override
		public String toString() {
			return "ClassName#methodName";
		}
	},
	CLASS_NAME_DOT_METHOD_NAME {
		@Override
		public String getBusinessTransactionName(String fullClassName, String methodName) {
			return ClassUtils.shorten(fullClassName) + "." + methodName;
		}

		@Override
		public String toString() {
			return "ClassName.methodName";
		}
	};

	public abstract String getBusinessTransactionName(String fullClassName, String methodName);
}
