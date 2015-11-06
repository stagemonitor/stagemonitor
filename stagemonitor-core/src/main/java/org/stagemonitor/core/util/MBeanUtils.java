package org.stagemonitor.core.util;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;

import com.codahale.metrics.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;

public class MBeanUtils {

	private static final Logger logger = LoggerFactory.getLogger(MBeanUtils.class);

	private static final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

	private static final Metric2Registry metricRegistry = Stagemonitor.getMetric2Registry();

	private MBeanUtils() {
	}

	/**
	 * @param objectName The object name pattern identifying the MBeans to be retrieved.
	 * @return The ObjectInstance object for the selected MBean.
	 * @throws java.util.NoSuchElementException
	 *                              There are no matching MBeans
	 * @throws NullPointerException The objectName parameter is null.
	 * @throws RuntimeException     The string passed as a parameter does not have the right format
	 * @see MBeanServer#queryMBeans(javax.management.ObjectName, javax.management.QueryExp)
	 */
	public static ObjectInstance queryMBean(String objectName) {
		return queryMBeans(objectName).iterator().next();
	}

	/**
	 * @param objectName The object name pattern identifying the MBeans to be retrieved.
	 * @return The ObjectInstance object for the selected MBean.
	 * @throws java.util.NoSuchElementException
	 *                              There are no matching MBeans
	 * @throws NullPointerException The objectName parameter is null.
	 * @throws RuntimeException     The string passed as a parameter does not have the right format
	 * @see MBeanServer#queryMBeans(javax.management.ObjectName, javax.management.QueryExp)
	 */
	public static Set<ObjectInstance> queryMBeans(String objectName) {
		try {
			return queryMBeans(new ObjectName(objectName), null);
		} catch (MalformedObjectNameException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets MBeans controlled by the MBean server. This method allows any of the following to be obtained:
	 * All MBeans, a set of MBeans specified by pattern matching on the ObjectName and/or a Query expression,
	 * a specific MBean. When the object name is null or no domain and key properties are specified,
	 * all objects are to be selected (and filtered if a query is specified).
	 * It returns the set of ObjectInstance objects (containing the ObjectName and the Java Class name)
	 * for the selected MBeans.
	 *
	 * @param name  The object name pattern identifying the MBeans to be retrieved.
	 *              If null or no domain and key properties are specified, all the MBeans registered will
	 *              be retrieved.
	 * @param query The query expression to be applied for selecting MBeans.
	 *              If null no query expression will be applied for selecting MBean
	 * @return A set containing the ObjectInstance objects for the selected MBeans.
	 *         If no MBean satisfies the query an empty list is returned.
	 */
	public static Set<ObjectInstance> queryMBeans(ObjectName objectName, QueryExp queryExp) {
		return mbeanServer.queryMBeans(objectName, queryExp);
	}

	/**
	 * Registers a MBean into the MetricRegistry
	 *
	 * @param objectInstance     The ObjectInstance
	 * @param mBeanAttributeName The attribute name of the MBean that should be collected
	 * @param metricName         The name of the metric in the MetricRegistry
	 */
	public static void registerMBean(final ObjectInstance objectInstance, final String mBeanAttributeName, MetricName metricName) {
		metricRegistry.register(metricName, new Gauge<Object>() {
			@Override
			public Object getValue() {
				try {
					return mbeanServer.getAttribute(objectInstance.getObjectName(), mBeanAttributeName);
				} catch (Exception e) {
					logger.warn(e.getMessage(), e);
					return null;
				}
			}
		});
	}
}
