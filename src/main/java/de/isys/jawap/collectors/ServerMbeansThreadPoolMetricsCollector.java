package de.isys.jawap.collectors;

import org.springframework.stereotype.Component;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.util.Set;

@Component
public class ServerMbeansThreadPoolMetricsCollector implements ThreadPoolMetricsCollector {

	private MBeanServer server = MBeanServerFactory.findMBeanServer(null).get(0);

	private ObjectName ajpThreadPoolMbeanName = null;
	private String threadPoolNamePattern;
	private String objectNameToQuery;

	private ObjectName getAjpThreadPoolMbeanName() throws IllegalStateException {
		if (ajpThreadPoolMbeanName == null) {
			Set<ObjectInstance> threadPoolMbeans = null;
			try {
				threadPoolMbeans = server.queryMBeans(new ObjectName(objectNameToQuery), null);
			} catch (MalformedObjectNameException e) {
				throw new RuntimeException(e);
			}

			for (ObjectInstance threadPoolMbean : threadPoolMbeans) {
				if (threadPoolMbean.getObjectName().toString().matches(threadPoolNamePattern)) {
					ajpThreadPoolMbeanName = threadPoolMbean.getObjectName();
					break;
				}
			}
			if (ajpThreadPoolMbeanName == null) {
				throw new IllegalStateException("ajpThreadPoolMbeanName was null!");
			}
		}
		return ajpThreadPoolMbeanName;
	}

	public int getThreadPoolSize() throws IllegalStateException {
		try {
			return ((Integer) server.getAttribute(getAjpThreadPoolMbeanName(), "currentThreadCount")).intValue();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public int getThreadPoolNumActiveThreads() throws IllegalStateException {
		try {
			return ((Integer) server.getAttribute(getAjpThreadPoolMbeanName(), "currentThreadsBusy")).intValue();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Integer getThreadPoolNumTasksPending() {
		return null;
	}

	@Override
	public int getMaxPoolSize() throws IllegalStateException {
		try {
			return ((Integer) server.getAttribute(getAjpThreadPoolMbeanName(), "maxThreads")).intValue();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setThreadPoolNamePattern(String threadPoolNamePattern) {
		this.threadPoolNamePattern = threadPoolNamePattern;
	}

	public void setObjectNameToQuery(String objectNameToQuery) {
		this.objectNameToQuery = objectNameToQuery;
	}
}
