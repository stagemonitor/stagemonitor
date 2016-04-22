package org.stagemonitor.requestmonitor;

import org.stagemonitor.core.util.StringUtils;

/**
 * Determines how a business transaction should be named
 */
public enum BusinessTransactionNamingStrategy {

	METHOD_NAME_SPLIT_CAMEL_CASE {
		@Override
		public String getBusinessTransationName(String fullClassName, String methodName) {
			return StringUtils.capitalize(StringUtils.splitCamelCase(methodName));
		}
	},
	CLASS_NAME_HASH_METHOD_NAME {
		@Override
		public String getBusinessTransationName(String fullClassName, String methodName) {
			return shorten(fullClassName) + "#" + methodName;
		}
	},
	CLASS_NAME_DOT_METHOD_NAME {
		@Override
		public String getBusinessTransationName(String fullClassName, String methodName) {
			return shorten(fullClassName) + "." + methodName;
		}
	};

	private static String shorten(String fullClassName) {
		return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
	}

	public abstract String getBusinessTransationName(String fullClassName, String methodName);
}
