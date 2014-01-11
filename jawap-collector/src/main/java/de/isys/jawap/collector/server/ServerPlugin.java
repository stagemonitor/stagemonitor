package de.isys.jawap.collector.server;

import com.codahale.metrics.MetricRegistry;
import de.isys.jawap.collector.core.Configuration;
import de.isys.jawap.collector.core.JawapPlugin;
import de.isys.jawap.collector.jvm.MBeanPooledResourceImpl;
import de.isys.jawap.collector.jvm.PooledResourceMetricsRegisterer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class ServerPlugin implements JawapPlugin {

	private final Log logger = LogFactory.getLog(getClass());

	boolean requiredPropertiesSet = true;

	@Override
	public void initializePlugin(MetricRegistry registry, Configuration conf) {
		final String objectName = getRequeredProperty("jawap.server.threadpool.objectName", conf);
		final String mbeanKeyPropertyName = getRequeredProperty("jawap.server.threadpool.mbeanKeyPropertyName", conf);
		final String mbeanActiveAttribute = getRequeredProperty("jawap.server.threadpool.mbeanActiveAttribute", conf);
		final String mbeanCountAttribute = getRequeredProperty("jawap.server.threadpool.mbeanCountAttribute", conf);
		final String mbeanMaxAttribute = getRequeredProperty("jawap.server.threadpool.mbeanMaxAttribute", conf);
		final String mbeanQueueAttribute = conf.getString("jawap.server.threadpool.mbeanQueueAttribute");
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
