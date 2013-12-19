package de.isys.jawap.collector.core;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import de.isys.jawap.collector.core.metrics.SortedTableConsoleReporter;
import de.isys.jawap.entities.MeasurementSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static de.isys.jawap.collector.core.monitor.ExecutionContextMonitor.encodeForGraphite;

public class ApplicationContext {

	private final static Log logger = LogFactory.getLog(ApplicationContext.class);
	private static MetricRegistry metricRegistry = new MetricRegistry();
	private static Configuration configuration = new Configuration();

	public static void setMeasurementSession(MeasurementSession measurementSession) {
		startMonitoring(measurementSession);
	}

	private static void startMonitoring(MeasurementSession measurementSession) {
		if (measurementSession.isInitialized()) {
			reportToGraphite(configuration.getGraphiteReportingInterval(), measurementSession);
			reportToConsole(configuration.getConsoleReportingInterval());
			if (configuration.reportToJMX()) {
				reportToJMX();
			}
		} else {
			logger.info("Measurement Session is not initialized " + measurementSession);
		}
	}

	private static void reportToGraphite(long reportingInterval, MeasurementSession measurementSession) {
		if (reportingInterval > 0) {
			final Graphite graphite = new Graphite(new InetSocketAddress(configuration.getGraphiteHostName(),
					configuration.getGraphitePort()));
			final GraphiteReporter reporter = GraphiteReporter.forRegistry(metricRegistry)
					.prefixedWith(getGraphitePrefix(measurementSession))
					.convertRatesTo(TimeUnit.SECONDS)
					.convertDurationsTo(TimeUnit.MILLISECONDS)
					.filter(MetricFilter.ALL)
					.build(graphite);
			reporter.start(reportingInterval, TimeUnit.SECONDS);
		}
	}

	private static String getGraphitePrefix(MeasurementSession measurementSession) {
		return name("jawap",
				encodeForGraphite(measurementSession.getApplicationName()),
				encodeForGraphite(measurementSession.getInstanceName()),
				encodeForGraphite(measurementSession.getHostName()));
	}

	private static void reportToConsole(long reportingInterval) {
		if (reportingInterval > 0) {
			final SortedTableConsoleReporter reporter = SortedTableConsoleReporter.forRegistry(metricRegistry)
					.convertRatesTo(TimeUnit.SECONDS)
					.convertDurationsTo(TimeUnit.MILLISECONDS)
					.build();
			reporter.start(reportingInterval, TimeUnit.SECONDS);
		}
	}

	private static void reportToJMX() {
		final JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).build();
		reporter.start();
	}

	public static MetricRegistry getMetricRegistry() {
		return metricRegistry;
	}

	public static void setMetricRegistry(MetricRegistry metricRegistry) {
		ApplicationContext.metricRegistry = metricRegistry;
	}

	public static Configuration getConfiguration() {
		return configuration;
	}
}
