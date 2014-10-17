package org.stagemonitor.os;

import com.codahale.metrics.MetricRegistry;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.Sigar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.configuration.ConfigurationSource;
import org.stagemonitor.core.configuration.SimpleSource;
import org.stagemonitor.core.rest.RestClient;
import org.stagemonitor.os.metrics.AbstractSigarMetricSet;
import org.stagemonitor.os.metrics.CpuMetricSet;
import org.stagemonitor.os.metrics.EmptySigarMetricSet;
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

public class OsPlugin implements StagemonitorPlugin {

	private final static Logger logger = LoggerFactory.getLogger(OsPlugin.class);

	private final Sigar sigar;

	public OsPlugin() throws IOException {
		this.sigar = newSigar();
	}

	public static Sigar newSigar() throws IOException {
		System.setProperty("java.library.path", NativeUtils.getLibraryPath(getSigarBindings(), "sigar"));
		return new Sigar();
	}

	public OsPlugin(Sigar sigar) {
		this.sigar = sigar;
	}

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) throws Exception {
		final CorePlugin config = configuration.getConfig(CorePlugin.class);
		final String elasticsearchUrl = config.getElasticsearchUrl();
		if (elasticsearchUrl != null && !elasticsearchUrl.isEmpty()) {
			RestClient.sendGrafanaDashboardAsync(elasticsearchUrl, "CPU.json");
			RestClient.sendGrafanaDashboardAsync(elasticsearchUrl, "Filesystem.json");
			RestClient.sendGrafanaDashboardAsync(elasticsearchUrl, "Memory.json");
			RestClient.sendGrafanaDashboardAsync(elasticsearchUrl, "Network.json");
			RestClient.sendGrafanaDashboardAsync(elasticsearchUrl, "OS Overview.json");
		}

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
	private AbstractSigarMetricSet<?> init(AbstractSigarMetricSet<?> metrics) {
		try {
			metrics.getSnapshot();
			return metrics;
		} catch (RuntimeException e) {
			logger.warn(e.getMessage() + ". (This exception is ignored)", e);
			return new EmptySigarMetricSet();
		}
	}

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		return Collections.emptyList();
	}

	public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {
		Stagemonitor.startMonitoring(getMeasurementSession(), getConfiguration(args));
		while (!Thread.currentThread().isInterrupted()) {
			Thread.sleep(100);
		}
	}

	static MeasurementSession getMeasurementSession() {
		final CorePlugin corePlugin = Stagemonitor.getConfiguration(CorePlugin.class);
		String applicationName = corePlugin.getApplicationName() != null ? corePlugin.getApplicationName() : "os";
		String instanceName = corePlugin.getInstanceName() != null ? corePlugin.getInstanceName() : "host";
		return new MeasurementSession(applicationName, getHostName(), instanceName);
	}

	static ConfigurationSource getConfiguration(String[] args) {
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

	static String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return getHostNameFromEnv();
		}
	}

	static String getHostNameFromEnv() {
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
