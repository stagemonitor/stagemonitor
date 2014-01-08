package de.isys.jawap.collector.server;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import de.isys.jawap.collector.core.JawapPlugin;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Set;

public class ServerPlugin implements JawapPlugin {

	private final Log logger = LogFactory.getLog(getClass());
	private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

	@Override
	public void initializePlugin(MetricRegistry registry) {
		try {
			final Set<ObjectInstance> objectInstances = server.queryMBeans(new ObjectName("Catalina:type=ThreadPool,*"), null);
			for (final ObjectInstance objectInstance : objectInstances) {
				// TODO part of objectname as metric name
				final String prefix = String.format("server.threadpool.%s", objectInstance.getObjectName().getKeyProperty("name")).replace("\"", "");
				registerAttributeAsMetric(registry, objectInstance, prefix + ".active", "currentThreadsBusy");
				registerAttributeAsMetric(registry, objectInstance, prefix + ".count", "currentThreadCount");
				registerAttributeAsMetric(registry, objectInstance, prefix + ".max", "maxThreads");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void registerAttributeAsMetric(MetricRegistry registry, final ObjectInstance objectInstance, String metricName, final String mbeanAttribute) {
		registry.register(metricName, new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				try {
					return (Integer) server.getAttribute(objectInstance.getObjectName(), mbeanAttribute);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					return null;
				}
			}
		});
	}
}
