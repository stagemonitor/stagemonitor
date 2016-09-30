package org.stagemonitor.jdbc.p6spy;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.P6Logger;
import com.uber.jaeger.context.TracingUtils;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.jdbc.JdbcPlugin;
import org.stagemonitor.requestmonitor.ExternalRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;

import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

public class StagemonitorP6Logger implements P6Logger {

	private final JdbcPlugin jdbcPlugin;
	private final RequestMonitor requestMonitor;
	private RequestMonitorPlugin requestMonitorPlugin;

	public StagemonitorP6Logger() {
		this(Stagemonitor.getConfiguration());
	}

	public StagemonitorP6Logger(Configuration configuration) {
		this.jdbcPlugin = configuration.getConfig(JdbcPlugin.class);
		requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
		requestMonitor = requestMonitorPlugin.getRequestMonitor();
	}

	@Override
	public void logSQL(final int connectionId, String now, long elapsed, Category category, String prepared, String sql) {
		final RequestTrace requestTrace = RequestMonitor.get().getRequestTrace();
		if (requestTrace == null) {
			return;
		}

		final String externalRequestAttribute = "jdbc" + connectionId;
		if (category == Category.STATEMENT) {
			createExternalRequest(externalRequestAttribute, requestTrace, elapsed, prepared, sql);
		} else {
			updateExternalRequest(externalRequestAttribute, requestTrace, elapsed);
		}
	}

	private void createExternalRequest(final String externalRequestAttribute, RequestTrace requestTrace, long elapsed, String prepared, String sql) {
		if (StringUtils.isNotEmpty(prepared)) {
			sql = getSql(prepared, sql);
			String method = sql.substring(0, sql.indexOf(' ')).toUpperCase();
			final long nowNanos = System.nanoTime();
			final Span span = requestMonitorPlugin.getTracer().buildSpan("jdbc_query")
					.asChildOf(TracingUtils.getTraceContext().getCurrentSpan())
					.withStartTimestamp(TimeUnit.NANOSECONDS.toMicros(nowNanos - elapsed))
					.start();
			span.finish(nowNanos);
			Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_CLIENT);
			final ExternalRequest jdbcRequest = new ExternalRequest("jdbc", method, elapsed, sql);
			requestMonitor.trackExternalRequest(jdbcRequest);
			requestTrace.addRequestAttribute(externalRequestAttribute, jdbcRequest);
		}
	}

	private void updateExternalRequest(String externalRequestAttribute, RequestTrace requestTrace, long elapsed) {
		ExternalRequest externalRequest = (ExternalRequest) requestTrace.getRequestAttribute(externalRequestAttribute);
		if (externalRequest != null) {
			requestTrace.addTimeToExternalRequest(externalRequest, elapsed);
		}
	}

	private String getSql(String prepared, String sql) {
		if (StringUtils.isEmpty(sql) || !jdbcPlugin.isCollectPreparedStatementParameters()) {
			sql = prepared;
		}
		return sql.trim();
	}

	@Override
	public void logException(Exception e) {
	}

	@Override
	public void logText(String text) {
	}

	@Override
	public boolean isCategoryEnabled(Category category) {
		return Category.STATEMENT.equals(category) || Category.RESULTSET.equals(category) || Category.RESULT.equals(category);
	}
}
