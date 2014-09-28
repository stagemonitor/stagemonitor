package org.stagemonitor.os;

import com.codahale.metrics.MetricRegistry;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Sigar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.StageMonitor;
import org.stagemonitor.core.StageMonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OsPlugin implements StageMonitorPlugin {

	private final static Logger logger = LoggerFactory.getLogger(OsPlugin.class);

	private final Sigar sigar;

	public OsPlugin() {
		this(new Sigar());
	}

	public OsPlugin(Sigar sigar) {
		this.sigar = sigar;
	}

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) throws Exception {
		metricRegistry.registerAll(new CpuMetricSet(sigar.getCpuPerc(), sigar.getCpuInfoList()[0]));
		metricRegistry.registerAll(new MemoryMetricSet(sigar.getMem()));
		metricRegistry.registerAll(new SwapMetricSet(sigar.getSwap()));
		metricRegistry.registerAll(new NetworkMetricSet(sigar.getTcp()));
		for (Map.Entry<String, FileSystem> e : (Set<Map.Entry<String, FileSystem>>) sigar.getFileSystemMap().entrySet()) {
			final FileSystem fs = e.getValue();
			if (fs.getType() == FileSystem.TYPE_LOCAL_DISK) {
				metricRegistry.registerAll(new FileSystemMetricSet(e.getKey(), sigar.getFileSystemUsage(e.getKey())));
			}
		}
	}

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		return Collections.emptyList();
	}

	public static void main(String[] args) throws UnknownHostException, InterruptedException {
		final CorePlugin corePlugin = StageMonitor.getConfiguration(CorePlugin.class);
		String applicationName = corePlugin.getApplicationName() != null ? corePlugin.getApplicationName() : "os";
		String instanceName = corePlugin.getInstanceName() != null ? corePlugin.getApplicationName() : "host";
		StageMonitor.startMonitoring(new MeasurementSession(applicationName, InetAddress.getLocalHost().getHostName(), instanceName));

		while (!Thread.currentThread().isInterrupted()) {
			Thread.sleep(100);
		}
	}
}
