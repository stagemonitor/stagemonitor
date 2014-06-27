package org.stagemonitor.jvm;


import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.lang.management.ManagementFactory;

public class CpuUtilisationWatch {

	private final ProcessCpuTime processCpuTime;

	private Long nanoBefore;
	private Long cpuBefore;

	public interface ProcessCpuTime {
		long getProcessCpuTime();
	}

	public CpuUtilisationWatch() throws ClassNotFoundException, IOException {
		MBeanServerConnection mbsc = ManagementFactory.getPlatformMBeanServer();
		Class.forName("com.sun.management.OperatingSystemMXBean");
		final com.sun.management.OperatingSystemMXBean osMxBean = ManagementFactory.newPlatformMXBeanProxy(mbsc, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
				com.sun.management.OperatingSystemMXBean.class);
		processCpuTime = new ProcessCpuTime() {
			@Override
			public long getProcessCpuTime() {
				return osMxBean.getProcessCpuTime();
			}
		};
	}

	public CpuUtilisationWatch(ProcessCpuTime processCpuTime) {
		this.processCpuTime = processCpuTime;
	}

	/**
	 * Starts or restarts the watch
	 */
	public void start() {
		nanoBefore = System.nanoTime();
		cpuBefore = processCpuTime.getProcessCpuTime();
	}

	/**
	 * Gets the average CPU utilisation since the last call of {@link CpuUtilisationWatch#start()}
	 *
	 * @return the average CPU utilisation since the last call of {@link CpuUtilisationWatch#start()}
	 */
	public float getCpuUsagePercent() {
		if (nanoBefore == null || cpuBefore == null) {
			throw new IllegalStateException("nanoBefore or cpuBefore where null: nanoBefore: " + nanoBefore + ", " +
					" cpuBefore: " + cpuBefore);
		}
		long cpuAfter = processCpuTime.getProcessCpuTime();
		long nanoAfter = System.nanoTime();

		float utilisation = ((float)(cpuAfter - cpuBefore)) / (nanoAfter - nanoBefore);
		return utilisation / Runtime.getRuntime().availableProcessors();
	}
}
