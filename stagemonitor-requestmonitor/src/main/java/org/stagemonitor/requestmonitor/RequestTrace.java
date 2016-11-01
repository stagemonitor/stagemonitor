package org.stagemonitor.requestmonitor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;

import java.util.Date;

import io.opentracing.NoopTracer;
import io.opentracing.Span;

/**
 * A request trace is a data structure containing all the important information about a request.
 *
 * @deprecated use {@link io.opentracing.Span}
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Deprecated
public class RequestTrace {

	private String name;
	@JsonProperty("@timestamp")
	private final String timestamp;
	private String username;
	private String disclosedUserName;
	private String clientIp;
	@JsonIgnore
	protected Span span = new NoopTracer().buildSpan(null).start();

	public RequestTrace(String requestId) {
		this(requestId, Stagemonitor.getMeasurementSession(), Stagemonitor.getPlugin(RequestMonitorPlugin.class));
	}

	public RequestTrace(String requestId, MeasurementSession measurementSession, RequestMonitorPlugin requestMonitorPlugin) {
		this.timestamp = StringUtils.dateAsIsoString(new Date());
	}

	/**
	 * The name of the request (e.g. 'Show Item Detail').
	 * <p/>
	 * If the name is not set when the requests ends, it won't be considered for the measurements and reportings.
	 *
	 * @return The name of the request
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the request (e.g. 'Show Item Detail'). The name can be overridden and set any time.
	 *
	 * @param name the name of the request
	 */
	public void setName(String name) {
		span.setOperationName(name);
		this.name = name;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setUsername(String username) {
		this.username = username;
		span.setTag("username", username);
	}

	void setDisclosedUserName(String disclosedUserName) {
		this.disclosedUserName = disclosedUserName;
		span.setTag("disclosedUserName", username);
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	public String getUsername() {
		return username;
	}

	public String getDisclosedUserName() {
		return disclosedUserName;
	}

	public String getClientIp() {
		return clientIp;
	}

	public long getTimestampEnd() {
		return 0;
	}

	public String toJson() {
		return JsonUtils.toJson(this, "callStack");
	}

	public void setSpan(Span span) {
		this.span = span;
	}
}
