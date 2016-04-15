package org.stagemonitor.jdbc;

import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import java.sql.Connection;

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
		ElementMatcher.Junction<TypeDescription> matcher = none();
		for (String impl : configuration.getConfig(JdbcPlugin.class).getDataSourceImplementations()) {
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
	public void onIgnored(TypeDescription typeDescription) {
		if (DEBUG_INSTRUMENTATION && typeDescription.getName().contains("DataSource")) {
			logger.info("IGNORE {}", typeDescription.getName());
		}
	}
}
