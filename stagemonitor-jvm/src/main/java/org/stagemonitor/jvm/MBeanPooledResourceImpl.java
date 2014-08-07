package org.stagemonitor.jvm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MBeanPooledResourceImpl implements PooledResource {

	private static final Logger logger = LoggerFactory.getLogger(MBeanPooledResourceImpl.class);

	private static final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
	private final String name;
	private ObjectName objectName;
	private final String mbeanActiveAttribute;
	private final String mbeanCountAttribute;
	private final String mbeanMaxAttribute;
	private final String mbeanQueueAttribute;

	public MBeanPooledResourceImpl(String prefix, ObjectName objectName, String mbeanKeyPropertyName,
								   String mbeanActiveAttribute, String mbeanCountAttribute,
								   String mbeanMaxAttribute, String mbeanQueueAttribute) {
		this.name = prefix + "." + objectName.getKeyProperty(mbeanKeyPropertyName);
		this.objectName = objectName;
		this.mbeanActiveAttribute = mbeanActiveAttribute;
		this.mbeanCountAttribute = mbeanCountAttribute;
		this.mbeanMaxAttribute = mbeanMaxAttribute;
		this.mbeanQueueAttribute = mbeanQueueAttribute;
	}

	public static List<MBeanPooledResourceImpl> of(String objectName, String metricPrefix, String mbeanKeyPropertyName,
												   String mbeanActiveAttribute, String mbeanCountAttribute,
												   String mbeanMaxAttribute) {
		return of(objectName, metricPrefix, mbeanKeyPropertyName, mbeanActiveAttribute, mbeanCountAttribute,
				mbeanMaxAttribute, null);
	}

	public static List<MBeanPooledResourceImpl> of(String objectName, String metricPrefix, String mbeanKeyPropertyName,
												   String mbeanActiveAttribute, String mbeanCountAttribute,
												   String mbeanMaxAttribute, String mbeanQueueAttribute) {
		try {
			final Set<ObjectInstance> objectInstances = server.queryMBeans(new ObjectName(objectName), null);
			List<MBeanPooledResourceImpl> pools = new ArrayList<MBeanPooledResourceImpl>(objectInstances.size());
			for (final ObjectInstance objectInstance : objectInstances) {
				pools.add(new MBeanPooledResourceImpl(metricPrefix, objectInstance.getObjectName(),
						mbeanKeyPropertyName, mbeanActiveAttribute, mbeanCountAttribute, mbeanMaxAttribute,
						mbeanQueueAttribute));
			}
			return pools;
		} catch (Exception e) {
			logger.warn(e.getMessage() + " (this exception is ignored)", e);
			return Collections.emptyList();
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getMaxPoolSize() {
		try {
			return (Integer) server.getAttribute(objectName, mbeanMaxAttribute);
		} catch (Exception e) {
			logger.warn(e.getMessage() + " (this exception is ignored)", e);
			return -1;
		}
	}

	@Override
	public int getThreadPoolSize() {
		try {
			return (Integer) server.getAttribute(objectName, mbeanCountAttribute);
		} catch (Exception e) {
			logger.warn(e.getMessage() + " (this exception is ignored)", e);
			return -1;
		}
	}

	@Override
	public int getThreadPoolNumActiveThreads() {
		try {
			return (Integer) server.getAttribute(objectName, mbeanActiveAttribute);
		} catch (Exception e) {
			logger.warn(e.getMessage() + " (this exception is ignored)", e);
			return -1;
		}
	}

	@Override
	public Integer getThreadPoolNumTasksPending() {
		if (mbeanQueueAttribute == null) {
			return null;
		}
		try {
			return (Integer) server.getAttribute(objectName, mbeanQueueAttribute);
		} catch (Exception e) {
			logger.warn(e.getMessage() + " (this exception is ignored)", e);
			return null;
		}
	}
}
