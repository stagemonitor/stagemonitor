package org.stagemonitor.core.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class TimeUtils {

	private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	private static final boolean isCurrentThreadCpuTimeSupported = threadMXBean.isCurrentThreadCpuTimeSupported();

	private TimeUtils() {
	}

	public static long getCpuTime() {
		return isCurrentThreadCpuTimeSupported ? threadMXBean.getCurrentThreadCpuTime() : 0L;
	}
}
