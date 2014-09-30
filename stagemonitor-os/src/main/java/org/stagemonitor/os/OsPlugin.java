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
import org.stagemonitor.core.configuration.ConfigurationSource;
import org.stagemonitor.core.configuration.SimpleSource;
import org.stagemonitor.core.rest.RestClient;
import org.stagemonitor.os.metrics.AbstractSigarMetricSet;
import org.stagemonitor.os.metrics.CpuMetricSet;
import org.stagemonitor.os.metrics.FileSystemMetricSet;
import org.stagemonitor.os.metrics.MemoryMetricSet;
import org.stagemonitor.os.metrics.NetworkMetricSet;
import org.stagemonitor.os.metrics.SwapMetricSet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OsPlugin implements StageMonitorPlugin {

	private final static Logger logger = LoggerFactory.getLogger(OsPlugin.class);

	private final Sigar sigar;

	public OsPlugin() throws IOException {
		System.setProperty("java.library.path", NativeUtils.getLibraryPath(getSigarBindings(), "sigar"));
		this.sigar = new Sigar();
	}

	public OsPlugin(Sigar sigar) {
		this.sigar = sigar;
	}

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) throws Exception {
		final CorePlugin config = configuration.getConfig(CorePlugin.class);
		RestClient.sendGrafanaDashboardAsync(config.getElasticsearchUrl(), "CPU.json");
		RestClient.sendGrafanaDashboardAsync(config.getElasticsearchUrl(), "Filesystem.json");
		RestClient.sendGrafanaDashboardAsync(config.getElasticsearchUrl(), "Memory.json");
		RestClient.sendGrafanaDashboardAsync(config.getElasticsearchUrl(), "Network.json");
		RestClient.sendGrafanaDashboardAsync(config.getElasticsearchUrl(), "OS Overview.json");

		try {
			metricRegistry.registerAll(init(new CpuMetricSet(sigar, sigar.getCpuInfoList()[0])));
			metricRegistry.registerAll(init(new MemoryMetricSet(sigar)));
			metricRegistry.registerAll(init(new SwapMetricSet(sigar)));
			for (String ifname : sigar.getNetInterfaceList()) {
				metricRegistry.registerAll(init(new NetworkMetricSet(ifname, sigar)));
			}
			@SuppressWarnings("unchecked")
			final Set<Map.Entry<String, FileSystem>> entries = (Set<Map.Entry<String, FileSystem>>) sigar.getFileSystemMap().entrySet();
			for (Map.Entry<String, FileSystem> e : entries) {
				final FileSystem fs = e.getValue();
				if (fs.getType() == FileSystem.TYPE_LOCAL_DISK) {
					metricRegistry.registerAll(init(new FileSystemMetricSet(e.getKey(), sigar)));
				}
			}
		} catch (UnsatisfiedLinkError e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * initializing by calling getSnapshot helps to avoid a strange npe
	 */
	private <T extends AbstractSigarMetricSet<?>> T init(T metrics) {
		metrics.getSnapshot();
		return metrics;
	}

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		return Collections.emptyList();
	}

	public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {

		final CorePlugin corePlugin = StageMonitor.getConfiguration(CorePlugin.class);
		String applicationName = corePlugin.getApplicationName() != null ? corePlugin.getApplicationName() : "os";
		String instanceName = corePlugin.getInstanceName() != null ? corePlugin.getApplicationName() : "host";
		StageMonitor.startMonitoring(new MeasurementSession(applicationName, getHostName(), instanceName), getConfiguration(args));

		while (!Thread.currentThread().isInterrupted()) {
			Thread.sleep(100);
		}
	}

	private static ConfigurationSource getConfiguration(String[] args) {
		final SimpleSource source = new SimpleSource("Process Arguments");
		for (String arg : args) {
			if (!arg.matches("(.+)=(.+)")) {
				throw new IllegalArgumentException("Illegal argument '" + arg +
						"'. Arguments must be in form '<config-key>=<config-value>'");
			}
			final String[] split = arg.split("=");
			source.add(split[0], split[1]);
		}
		return source;
	}

	private static String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			// try environment properties.
			String host = System.getenv("COMPUTERNAME");
			if (host != null) {
				return host;
			}
			host = System.getenv("HOSTNAME");
			if (host != null) {
				return host;
			}
			return null;
		}
	}

	private static List<String> getSigarBindings() {
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
		return sigarBindings;
	}
}
