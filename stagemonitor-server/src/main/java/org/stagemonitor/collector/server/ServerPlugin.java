package org.stagemonitor.collector.server;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.collector.core.Configuration;
import org.stagemonitor.collector.core.StageMonitorPlugin;
import org.stagemonitor.collector.jvm.MBeanPooledResourceImpl;
import org.stagemonitor.collector.jvm.PooledResourceMetricsRegisterer;

import java.util.List;

public class ServerPlugin implements StageMonitorPlugin {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	boolean requiredPropertiesSet = true;

	@Override
	public void initializePlugin(MetricRegistry registry, Configuration conf) {
		final String objectName = getRequeredProperty("stagemonitor.server.threadpool.objectName", conf);
		final String mbeanKeyPropertyName = getRequeredProperty("stagemonitor.server.threadpool.mbeanKeyPropertyName", conf);
		final String mbeanActiveAttribute = getRequeredProperty("stagemonitor.server.threadpool.mbeanActiveAttribute", conf);
		final String mbeanCountAttribute = getRequeredProperty("stagemonitor.server.threadpool.mbeanCountAttribute", conf);
		final String mbeanMaxAttribute = getRequeredProperty("stagemonitor.server.threadpool.mbeanMaxAttribute", conf);
		final String mbeanQueueAttribute = conf.getString("stagemonitor.server.threadpool.mbeanQueueAttribute");
		if (requiredPropertiesSet) {
			final List<MBeanPooledResourceImpl> pools = MBeanPooledResourceImpl.of(objectName,
					"server.threadpool", mbeanKeyPropertyName, mbeanActiveAttribute, mbeanCountAttribute,
					mbeanMaxAttribute, mbeanQueueAttribute);
			PooledResourceMetricsRegisterer.registerPooledResources(pools, registry);
		}
	}

	private String getRequeredProperty(String propertyKey, Configuration conf) {
		String requredProperty = conf.getString(propertyKey);
		if (requredProperty == null || requredProperty.isEmpty()) {
			logger.info(propertyKey + " is empty, Server Plugin deactivated");
			requiredPropertiesSet = false;
		}
		return requredProperty;
	}

}
