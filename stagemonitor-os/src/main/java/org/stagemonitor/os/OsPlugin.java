package org.stagemonitor.os;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.NetRoute;
import org.hyperic.sigar.Sigar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.grafana.GrafanaClient;
import org.stagemonitor.os.metrics.AbstractSigarMetricSet;
import org.stagemonitor.os.metrics.CpuMetricSet;
import org.stagemonitor.os.metrics.EmptySigarMetricSet;
import org.stagemonitor.os.metrics.FileSystemMetricSet;
import org.stagemonitor.os.metrics.MemoryMetricSet;
import org.stagemonitor.os.metrics.NetworkMetricSet;
import org.stagemonitor.os.metrics.SwapMetricSet;

public class OsPlugin extends StagemonitorPlugin  {

	private final static Logger logger = LoggerFactory.getLogger(OsPlugin.class);

	private Sigar sigar;

	@Override
	public void initializePlugin(StagemonitorPlugin.InitArguments initArguments) throws Exception {
		final CorePlugin corePlugin = initArguments.getPlugin(CorePlugin.class);

		ElasticsearchClient elasticsearchClient = corePlugin.getElasticsearchClient();
		if (corePlugin.isReportToGraphite()) {
			elasticsearchClient.sendGrafana1DashboardAsync("grafana/Grafana1GraphiteCPU.json");
			elasticsearchClient.sendGrafana1DashboardAsync("grafana/Grafana1GraphiteFilesystem.json");
			elasticsearchClient.sendGrafana1DashboardAsync("grafana/Grafana1GraphiteMemory.json");
			elasticsearchClient.sendGrafana1DashboardAsync("grafana/Grafana1GraphiteNetwork.json");
			elasticsearchClient.sendGrafana1DashboardAsync("grafana/Grafana1GraphiteOSOverview.json");
		}
		if (corePlugin.isReportToElasticsearch()) {
			final GrafanaClient grafanaClient = corePlugin.getGrafanaClient();
			grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchHostDashboard.json");
			elasticsearchClient.sendBulkAsync("kibana/HostDashboard.bulk");
		}

		if (sigar == null) {
			if (!SigarNativeBindingLoader.loadNativeSigarBindings()) {
				// redeploys are a problem, because the native libs can only be loaded by one class loader
				// this would lead to a UnsatisfiedLinkError: Native Library sigar already loaded in another class loader
				throw new RuntimeException("The OsPlugin only works with one application per JVM " +
						"and does not work after a redeploy");
			}
			sigar = newSigar();
		}
		initArguments.getMetricRegistry().registerAll(init(new CpuMetricSet(sigar, sigar.getCpuInfoList()[0])));
		initArguments.getMetricRegistry().registerAll(init(new MemoryMetricSet(sigar)));
		initArguments.getMetricRegistry().registerAll(init(new SwapMetricSet(sigar)));

		Set<String> routedNetworkInterfaces = new HashSet<String>();
		for (NetRoute netRoute : sigar.getNetRouteList()) {
			routedNetworkInterfaces.add(netRoute.getIfname());
		}
		for (String ifname : routedNetworkInterfaces) {
			initArguments.getMetricRegistry().registerAll(init(new NetworkMetricSet(ifname, sigar)));
		}
		@SuppressWarnings("unchecked")
		final Set<Map.Entry<String, FileSystem>> entries = (Set<Map.Entry<String, FileSystem>>) sigar.getFileSystemMap().entrySet();
		for (Map.Entry<String, FileSystem> e : entries) {
			final FileSystem fs = e.getValue();
			if (fs.getType() == FileSystem.TYPE_LOCAL_DISK || fs.getType() == FileSystem.TYPE_NETWORK) {
				initArguments.getMetricRegistry().registerAll(init(new FileSystemMetricSet(e.getKey(), sigar)));
			}
		}
	}

	@Override
	public void onShutDown() {
		if (sigar != null) {
			sigar.close();
			sigar = null;
		}
	}

	private Sigar newSigar() throws Exception {
		try {
			final Sigar s = new Sigar();
			s.getCpuInfoList();
			return s;
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

	public static void main(String[] args) throws InterruptedException {
		OsConfigurationSourceInitializer.addConfigurationSource(args);
		Stagemonitor.startMonitoring(getMeasurementSession());
		System.out.println("Interrupt (Ctrl + C) to exit");
		Thread.currentThread().join();
	}

	static MeasurementSession getMeasurementSession() {
		final CorePlugin corePlugin = Stagemonitor.getPlugin(CorePlugin.class);
		String applicationName = corePlugin.getApplicationName() != null ? corePlugin.getApplicationName() : "os";
		String instanceName = corePlugin.getInstanceName() != null ? corePlugin.getInstanceName() : "host";
		return new MeasurementSession(applicationName, corePlugin.getHostName(), instanceName);
	}

	@Override
	public void registerWidgetMetricTabPlugins(WidgetMetricTabPluginsRegistry widgetMetricTabPluginsRegistry) {
		widgetMetricTabPluginsRegistry.addWidgetMetricTabPlugin("/stagemonitor/static/tabs/metrics/os-metrics");
	}

	public Sigar getSigar() {
		return sigar;
	}
}
