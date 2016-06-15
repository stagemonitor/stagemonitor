package org.stagemonitor.jdbc;

import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.isSubTypeOf;
import static net.bytebuddy.matcher.ElementMatchers.nameContains;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import java.sql.Connection;
import java.util.Collection;

import javax.sql.DataSource;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;

public class ConnectionMonitoringTransformer extends StagemonitorByteBuddyTransformer {

	private static final Logger logger = LoggerFactory.getLogger(ConnectionMonitoringTransformer.class);

	protected static final ConnectionMonitor connectionMonitor;

	private static final boolean active;

	static {
		active = ConnectionMonitor.isActive(configuration.getConfig(CorePlugin.class));
		if (active) {
			connectionMonitor = new ConnectionMonitor(configuration, Stagemonitor.getMetric2Registry());
		} else {
			connectionMonitor = null;
		}
	}

	@Override
	public ElementMatcher.Junction<TypeDescription> getIncludeTypeMatcher() {
		final Collection<String> dataSourceImplementations = configuration.getConfig(JdbcPlugin.class).getDataSourceImplementations();
		if (dataSourceImplementations.isEmpty()) {
			return nameContains("DataSource").and(isSubTypeOf(DataSource.class));
		}
		ElementMatcher.Junction<TypeDescription> matcher = none();
		for (String impl : dataSourceImplementations) {
			matcher = matcher.or(named(impl));
		}
		return matcher;
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return named("getConnection")
				.and(returns(Connection.class))
				.and(isPublic())
				.and(takesArguments(String.class, String.class).or(takesArguments(0)));
	}

	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader) {
		if (DEBUG_INSTRUMENTATION && typeDescription.getName().contains("DataSource")) {
			if (getTypeMatcher().matches(typeDescription)) {
				final boolean classLoaderMatches = getClassLoaderMatcher().matches(classLoader);
				logger.warn("IGNORE {} in {}. ClassLoader: {} matches: {}", typeDescription.getName(),
						getClass().getSimpleName(), classLoader, classLoaderMatches);

				if (!classLoaderMatches) {
					logger.warn("excluded by classloader matcher");
				} else if (!getRawMatcher().matches(typeDescription, classLoader, null, null, null)) {
					logger.warn("excluded by raw matcher");
				} else {
					logger.warn("excluded by global matcher");
				}
			} else {
				logger.info("IGNORE {} in {}", typeDescription.getName(), getClass().getSimpleName());
			}
		}
	}

	@Override
	public void beforeTransformation(TypeDescription typeDescription, ClassLoader classLoader) {
		if (DEBUG_INSTRUMENTATION && logger.isDebugEnabled()) {
			logger.info("TRANSFORM DataSource {} ({})", typeDescription.getName(), getClass().getSimpleName());
		}
	}
}
