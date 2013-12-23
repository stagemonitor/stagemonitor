package de.isys.jawap.collector.core;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import de.isys.jawap.collector.core.metrics.SortedTableConsoleReporter;
import de.isys.jawap.entities.MeasurementSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetSocketAddress;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static de.isys.jawap.util.GraphiteEncoder.encodeForGraphite;

public class ApplicationContext {

	private final static Log logger = LogFactory.getLog(ApplicationContext.class);
	private static Configuration configuration = new Configuration();

	public static void startMonitoring(MeasurementSession measurementSession) {
		if (measurementSession.isInitialized()) {
			initializePlugins();
			reportToGraphite(configuration.getGraphiteReportingInterval(), measurementSession);
			reportToConsole(configuration.getConsoleReportingInterval());
			if (configuration.reportToJMX()) {
				reportToJMX();
			}
		} else {
			logger.info("Measurement Session is not initialized " + measurementSession);
		}
	}

	private static void initializePlugins() {
		for (JawapPlugin jawapPlugin : ServiceLoader.load(JawapPlugin.class)) {
			jawapPlugin.initializePlugin();
		}
	}

	private static void reportToGraphite(long reportingInterval, MeasurementSession measurementSession) {
		if (reportingInterval > 0) {
			for (String plugin : SharedMetricRegistries.names()) {
				final Graphite graphite = new Graphite(new InetSocketAddress(configuration.getGraphiteHostName(),
						configuration.getGraphitePort()));
				GraphiteReporter.forRegistry(SharedMetricRegistries.getOrCreate(plugin))
						.prefixedWith(getGraphitePrefix(measurementSession, plugin))
						.convertRatesTo(TimeUnit.SECONDS)
						.convertDurationsTo(TimeUnit.MILLISECONDS)
						.filter(MetricFilter.ALL)
						.build(graphite)
						.start(reportingInterval, TimeUnit.SECONDS);
			}
		}
	}

	private static String getGraphitePrefix(MeasurementSession measurementSession, String plugin) {
		return name("jawap",
				encodeForGraphite(measurementSession.getApplicationName()),
				encodeForGraphite(measurementSession.getInstanceName()),
				encodeForGraphite(measurementSession.getHostName()),
				encodeForGraphite(plugin));
	}

	private static void reportToConsole(long reportingInterval) {
		if (reportingInterval > 0) {
			String pluginConsoleReporting = configuration.getPluginConsoleReporting();
			if (pluginConsoleReporting != null) {
				SortedTableConsoleReporter
						.forRegistry(SharedMetricRegistries.getOrCreate(pluginConsoleReporting))
						.convertRatesTo(TimeUnit.SECONDS)
						.convertDurationsTo(TimeUnit.MILLISECONDS)
						.build()
						.start(reportingInterval, TimeUnit.SECONDS);
			}
		}
	}

	private static void reportToJMX() {
		for (String plugin : SharedMetricRegistries.names()) {
			JmxReporter.forRegistry(SharedMetricRegistries.getOrCreate(plugin)).build().start();
		}
	}

	public static Configuration getConfiguration() {
		return configuration;
	}
}
