package org.stagemonitor.jdbc;

import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.nameEndsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import java.sql.Connection;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

public class ConnectionMonitoringTransformer extends StagemonitorByteBuddyTransformer {

	protected static ConnectionMonitor connectionMonitor;

	protected final Configuration configuration;

	protected final Metric2Registry metric2Registry;

	private final boolean active;

	public ConnectionMonitoringTransformer() throws NoSuchMethodException {
		configuration = Stagemonitor.getConfiguration();
		metric2Registry = Stagemonitor.getMetric2Registry();
		this.active = ConnectionMonitor.isActive(configuration.getConfig(CorePlugin.class));
		if (active) {
			connectionMonitor = new ConnectionMonitor(configuration, metric2Registry);
		}
	}

	@Override
	public ElementMatcher.Junction<TypeDescription> getIncludeTypeMatcher() {
		return nameEndsWith("DataSource");
	}

	@Override
	protected ElementMatcher.Junction<? super MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return named("getConnection")
				.and(returns(Connection.class))
				.and(isPublic())
				.and(takesArguments(String.class, String.class).or(takesArguments(0)));
	}

	@Override
	public boolean isActive() {
		return active;
	}

}
