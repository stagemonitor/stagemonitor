package org.stagemonitor.os;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codahale.metrics.MetricRegistry;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.NetRoute;
import org.hyperic.sigar.Sigar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.StagemonitorConfigurationSourceInitializer;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.source.ConfigurationSource;
import org.stagemonitor.core.configuration.source.SimpleSource;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.os.metrics.AbstractSigarMetricSet;
import org.stagemonitor.os.metrics.CpuMetricSet;
import org.stagemonitor.os.metrics.EmptySigarMetricSet;
import org.stagemonitor.os.metrics.FileSystemMetricSet;
import org.stagemonitor.os.metrics.MemoryMetricSet;
import org.stagemonitor.os.metrics.NetworkMetricSet;
import org.stagemonitor.os.metrics.SwapMetricSet;

public class OsPlugin extends StagemonitorPlugin implements StagemonitorConfigurationSourceInitializer {

	private final static Logger logger = LoggerFactory.getLogger(OsPlugin.class);
	private static ConfigurationSource argsConfigurationSource;

	private Sigar sigar;

	public OsPlugin() {
		try {
			loadNativeSigarBindings();
		} catch (Exception e) {
			logger.warn(e.getMessage() + " (this exception was ignored)", e);
		}
	}

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) throws Exception {
		final CorePlugin config = configuration.getConfig(CorePlugin.class);
		final String elasticsearchUrl = config.getElasticsearchUrl();
		if (elasticsearchUrl != null && !elasticsearchUrl.isEmpty()) {
			ElasticsearchClient elasticsearchClient = configuration.getConfig(CorePlugin.class).getElasticsearchClient();
			elasticsearchClient.sendGrafanaDashboardAsync("CPU.json");
			elasticsearchClient.sendGrafanaDashboardAsync("Filesystem.json");
			elasticsearchClient.sendGrafanaDashboardAsync("Memory.json");
			elasticsearchClient.sendGrafanaDashboardAsync("Network.json");
			elasticsearchClient.sendGrafanaDashboardAsync("OS Overview.json");
		}

		if (sigar == null) {
			sigar = newSigar();
		}
		metricRegistry.registerAll(init(new CpuMetricSet(sigar, sigar.getCpuInfoList()[0])));
		metricRegistry.registerAll(init(new MemoryMetricSet(sigar)));
		metricRegistry.registerAll(init(new SwapMetricSet(sigar)));

		Set<String> routedNetworkInterfaces = new HashSet<String>();
		for (NetRoute netRoute : sigar.getNetRouteList()) {
			routedNetworkInterfaces.add(netRoute.getIfname());
		}
		for (String ifname : routedNetworkInterfaces) {
			metricRegistry.registerAll(init(new NetworkMetricSet(ifname, sigar)));
		}
		@SuppressWarnings("unchecked")
		final Set<Map.Entry<String, FileSystem>> entries = (Set<Map.Entry<String, FileSystem>>) sigar.getFileSystemMap().entrySet();
		for (Map.Entry<String, FileSystem> e : entries) {
			final FileSystem fs = e.getValue();
			if (fs.getType() == FileSystem.TYPE_LOCAL_DISK || fs.getType() == FileSystem.TYPE_NETWORK) {
				metricRegistry.registerAll(init(new FileSystemMetricSet(e.getKey(), sigar)));
			}
		}
	}

	public static Sigar newSigar() throws Exception {
		final String libraryPath = loadNativeSigarBindings();
		try {
			final Sigar s = new Sigar();
			s.getCpuInfoList();
			return s;
		} catch (UnsatisfiedLinkError e) {
			logger.warn("Please add -Djava.library.path={} system property to resolve the UnsatisfiedLinkError", libraryPath);
			throw new RuntimeException(e);
		}
	}

	private static String loadNativeSigarBindings() throws Exception {
		return NativeUtils.addResourcesToLibraryPath(getSigarBindings(), "sigar");
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

	public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {
		argsConfigurationSource = getConfiguration(args);
		Stagemonitor.startMonitoring(getMeasurementSession());
		System.out.println("Press key to exit");
		System.in.read();
		Stagemonitor.shutDown();
	}

	static MeasurementSession getMeasurementSession() {
		final CorePlugin corePlugin = Stagemonitor.getConfiguration(CorePlugin.class);
		String applicationName = corePlugin.getApplicationName() != null ? corePlugin.getApplicationName() : "os";
		String instanceName = corePlugin.getInstanceName() != null ? corePlugin.getInstanceName() : "host";
		return new MeasurementSession(applicationName, MeasurementSession.getNameOfLocalHost(), instanceName);
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

	@Override
	public void modifyConfigurationSources(List<ConfigurationSource> configurationSources) {
		if (argsConfigurationSource != null) {
			configurationSources.add(0, argsConfigurationSource);
		}
	}

	@Override
	public void onConfigurationInitialized(Configuration configuration) throws Exception {
	}

	@Override
	public List<String> getPathsOfWidgetMetricTabPlugins() {
		return Arrays.asList("/stagemonitor/static/tabs/metrics/os-metrics");
	}

	public Sigar getSigar() {
		return sigar;
	}
}
