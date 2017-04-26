package org.stagemonitor.requestmonitor;

import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.util.StringUtils;

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
			return ClassUtils.shorten(fullClassName) + "#" + methodName;
		}
	},
	CLASS_NAME_DOT_METHOD_NAME {
		@Override
		public String getBusinessTransationName(String fullClassName, String methodName) {
			return ClassUtils.shorten(fullClassName) + "." + methodName;
		}
	};

	public abstract String getBusinessTransationName(String fullClassName, String methodName);
}
