package de.isys.jawap.collector.periodic;

// TODO remove hard dependency on com.sun package

import com.sun.management.OperatingSystemMXBean;

import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.lang.management.ManagementFactory;

public class CpuUtilisationWatch {

	private static final OperatingSystemMXBean osMBean;

	private Long nanoBefore;
	private Long cpuBefore;

	static {
		MBeanServerConnection mbsc = ManagementFactory.getPlatformMBeanServer();
		try {
			osMBean = ManagementFactory.newPlatformMXBeanProxy(mbsc, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
					OperatingSystemMXBean.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Starts or restarts the watch
	 */
	public synchronized void start() {
		nanoBefore = Long.valueOf(System.nanoTime());
		cpuBefore = Long.valueOf(osMBean.getProcessCpuTime());
	}

	/**
	 * Gets the average CPU utilisation since the last call of {@link CpuUtilisationWatch#start()}
	 *
	 * @return the average CPU utilisation since the last call of {@link CpuUtilisationWatch#start()}
	 */
	public synchronized float getCpuUsagePercent() {
		if (nanoBefore == null || cpuBefore == null) {
			throw new IllegalStateException("nanoBefore or cpuBefore where null: nanoBefore: " + nanoBefore + ", " +
					" cpuBefore: " + cpuBefore);
		}
		long cpuAfter = osMBean.getProcessCpuTime();
		long nanoAfter = System.nanoTime();

		float utilisation = ((cpuAfter - cpuBefore.longValue()) * 100F) / (nanoAfter - nanoBefore.longValue());
		return utilisation / Runtime.getRuntime().availableProcessors();
	}
}
