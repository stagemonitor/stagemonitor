package de.isys.jawap.collector.jdbc;

import de.isys.jawap.collector.core.ApplicationContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Aspect
public class ConnectionMonitorAspect {

	public static final String METRIC_PREFIX = "jdbc.getconnection.";
	public static final String TIME = ".time";
	public static final String COUNT = ".count";
	public ConcurrentMap<DataSource, String> dataSourceUrlMap = new ConcurrentHashMap<DataSource, String>();

	@Pointcut(value = "within(javax.sql.DataSource+) && execution(* javax.sql.DataSource.getConnection(..)) && this(dataSource)", argNames = "dataSource")
	public void dataSourceConnection(DataSource dataSource) {
	}

	@Pointcut(value = "dataSourceConnection(dataSource) && !beneathConnection()", argNames = "dataSource")
	public void topLevelDataSourceConnection(DataSource dataSource) {
	}

	@Pointcut("dataSourceConnection(*) || directConnection(*)")
	public void connection() {
	}

	@Pointcut("cflowbelow(connection())")
	public void beneathConnection() {
	}


	@Around(value = "execution(* javax.sql.DataSource.getConnection(..))")
	public Object aroundGetConnection(ProceedingJoinPoint pjp/*, DataSource dataSource*/) throws Throwable {

		/*if (!dataSourceUrlMap.containsKey(dataSource)) {
			final DatabaseMetaData metaData = dataSource.getConnection().getMetaData();
			dataSourceUrlMap.put(dataSource, metaData.getURL() + "-" + metaData.getUserName());
		}*/
//		return aroundDirectConnectionConnection(pjp, /*dataSourceUrlMap.get(dataSource)*/"test");
		System.out.println("test");
		return pjp.proceed();
	}

	@Pointcut(value = "(within(java.sql.Driver+) && execution(* java.sql.Driver.connect(..))  || within(java.sql.DriverManager+) && execution(* java.sql.DriverManager.getConnection(..))) && args(url, ..)", argNames = "url")
	public void directConnection(String url) {}


	@Pointcut(value = "directConnection(url) && !beneathConnection()", argNames = "url")
	public void topLevelDirectConnection(String url) {
	}

	@Around(value = "topLevelDirectConnection(url)", argNames = "pjp,url")
	public Object aroundDirectConnectionConnection(ProceedingJoinPoint pjp, String url) throws Throwable {
		long start = System.nanoTime();
		try {
			return pjp.proceed();
		} finally {
			long duration = System.nanoTime() - start;
			ApplicationContext.getMetricRegistry().counter(METRIC_PREFIX + url + COUNT).inc();
			ApplicationContext.getMetricRegistry().counter(METRIC_PREFIX + url + TIME).inc(TimeUnit.NANOSECONDS.toMillis(duration));
		}
	}
}
