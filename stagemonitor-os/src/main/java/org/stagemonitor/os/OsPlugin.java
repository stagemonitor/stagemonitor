package org.stagemonitor.os;

import com.codahale.metrics.MetricRegistry;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.Sigar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.StageMonitor;
import org.stagemonitor.core.StageMonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.configuration.SimpleSource;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
		for (String ifname : sigar.getNetInterfaceList()) {
			metricRegistry.registerAll(new NetworkMetricSet(ifname, sigar.getNetInterfaceStat(ifname)));
		}
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

	public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {
		List<String> sigarBindings = new ArrayList<String>();
		sigarBindings.add("/sigar/libsigar-amd64-freebsd-6.so");
		sigarBindings.add("/sigar/libsigar-amd64-linux.so");
		sigarBindings.add("/sigar/libsigar-amd64-solaris.so");
		sigarBindings.add("/sigar/libsigar-ia64-linux.so");
		sigarBindings.add("/sigar/libsigar-sparc64-solaris.so");
		sigarBindings.add("/sigar/libsigar-sparc-solaris.so");
		sigarBindings.add("/sigar/libsigar-universal64-macosx.dylib");
		sigarBindings.add("/sigar/libsigar-universal-macosx.dylib");
		sigarBindings.add("/sigar/libsigar-x86-freebsd-5.so");
		sigarBindings.add("/sigar/libsigar-x86-freebsd-6.so");
		sigarBindings.add("/sigar/libsigar-x86-linux.so");
		sigarBindings.add("/sigar/libsigar-x86-solaris.so");
		sigarBindings.add("/sigar/sigar-amd64-winnt.dll");
		sigarBindings.add("/sigar/sigar-x86-winnt.dll");
		final String libraryPath = NativeUtils.getLibraryPath(sigarBindings);
		logger.info(libraryPath);
		System.setProperty("java.library.path", System.getProperty("java.library.path")+";"+"D:\\tmp");

		StageMonitor.getConfiguration().addConfigurationSource(SimpleSource.forTest("stagemonitor.reporting.interval.console", "10"));
		final CorePlugin corePlugin = StageMonitor.getConfiguration(CorePlugin.class);
		String applicationName = corePlugin.getApplicationName() != null ? corePlugin.getApplicationName() : "os";
		String instanceName = corePlugin.getInstanceName() != null ? corePlugin.getApplicationName() : "host";
		StageMonitor.startMonitoring(new MeasurementSession(applicationName, InetAddress.getLocalHost().getHostName(), instanceName));

		while (!Thread.currentThread().isInterrupted()) {
			Thread.sleep(100);
		}
	}
}
