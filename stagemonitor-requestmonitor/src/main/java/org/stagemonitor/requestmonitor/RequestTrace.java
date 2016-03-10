package org.stagemonitor.requestmonitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.utils.IPAnonymizationUtils;

/**
 * A request trace is a data structure containing all the important information about a request.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestTrace {

	@JsonIgnore
	private final RequestMonitorPlugin requestMonitorPlugin;

	private final String id;
	private String name;
	@JsonIgnore
	private CallStackElement callStack;
	private long executionTime;
	private long executionTimeDb;
	private int executionCountDb;
	private long executionTimeCpu;
	private boolean error = false;
	@JsonProperty("@timestamp")
	private final String timestamp;
	@JsonIgnore
	private long timestampEnd;
	private Map<String, String> parameters;
	@JsonProperty("measurement_start")
	private final long measurementStart;
	private final String application;
	private final String host;
	private final String instance;
	private String exceptionMessage;
	private String exceptionClass;
	private String exceptionStackTrace;
	private String username;
	private String disclosedUserName;
	private String clientIp;
	private String uniqueVisitorId;
	private Map<String, Object> customProperties = new HashMap<String, Object>();
	@JsonIgnore
	private Map<String, Object> requestAttributes = new HashMap<String, Object>();

	public RequestTrace(String requestId) {
		this(requestId, Stagemonitor.getMeasurementSession(), Stagemonitor.getPlugin(RequestMonitorPlugin.class));
	}

	public RequestTrace(String requestId, MeasurementSession measurementSession, RequestMonitorPlugin requestMonitorPlugin) {
		this.requestMonitorPlugin = requestMonitorPlugin;
		this.id = requestId != null ? requestId : UUID.randomUUID().toString();
		this.measurementStart = measurementSession.getStartTimestamp();
		this.application = measurementSession.getApplicationName();
		this.host = measurementSession.getHostName();
		this.instance = measurementSession.getInstanceName();
		this.timestamp = StringUtils.dateAsIsoString(new Date());
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean failed) {
		this.error = failed;
	}

	public String getStatus() {
		return error ? "Error" : "OK";
	}

	public String getId() {
		return id;
	}

	public CallStackElement getCallStack() {
		return callStack;
	}

	public void setCallStack(CallStackElement callStack) {
		this.callStack = callStack;
	}

	@JsonProperty("callStack")
	public String getCallStackAscii() {
		if (callStack == null) {
			return null;
		}
		return callStack.toString(true);
	}

	public String getCallStackJson() {
		return JsonUtils.toJson(callStack);
	}

	/**
	 * The name of the request (e.g. 'Show Item Detail').
	 * <p/>
	 * If the name is not set when the requests ends, it won't be considered for the measurements and reportings.
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
		this.name = name;
	}

	public long getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
		timestampEnd = System.currentTimeMillis();
	}

	public long getExecutionTimeCpu() {
		return executionTimeCpu;
	}

	public void setExecutionTimeCpu(long executionTimeCpu) {
		this.executionTimeCpu = executionTimeCpu;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public String getApplication() {
		return application;
	}

	public String getHost() {
		return host;
	}

	public String getInstance() {
		return instance;
	}

	public String getExceptionMessage() {
		return exceptionMessage;
	}

	public void setExceptionMessage(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
	}

	public String getExceptionStackTrace() {
		return exceptionStackTrace;
	}

	public void setExceptionStackTrace(String exceptionStackTrace) {
		this.exceptionStackTrace = exceptionStackTrace;
	}

	public String getExceptionClass() {
		return exceptionClass;
	}

	public void setExceptionClass(String exceptionClass) {
		this.exceptionClass = exceptionClass;
	}

	public void setException(Exception e) {
		error = e != null;
		Throwable throwable = e;
		if (throwable != null) {
			if (requestMonitorPlugin.getUnnestExceptions().contains(throwable.getClass().getName())) {
				Throwable cause = throwable.getCause();
				if (cause != null) {
					throwable = cause;
				}
			}
			exceptionMessage = throwable.getMessage();
			exceptionClass = throwable.getClass().getCanonicalName();

			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw, true);
			throwable.printStackTrace(pw);
			exceptionStackTrace = sw.getBuffer().toString();
		}
	}

	public String getUsername() {
		return username;
	}

	public void setAndAnonymizeUserNameAndIp(String username, String clientIp) {
		if (requestMonitorPlugin.isPseudonymizeUserNames()) {
			this.username = StringUtils.sha1Hash(username);
		} else {
			this.username = username;
		}
		final boolean disclose = requestMonitorPlugin.getDiscloseUsers().contains(this.username);
		if (disclose) {
			this.disclosedUserName = username;
		}
		if (clientIp != null) {
			setAndAnonymizeClientIp(clientIp, disclose);
		}
	}

	private void setAndAnonymizeClientIp(String clientIp, boolean disclose) {
		if (requestMonitorPlugin.isAnonymizeIPs() && !disclose) {
			this.clientIp = IPAnonymizationUtils.anonymize(clientIp);
		} else {
			this.clientIp = clientIp;
		}
	}

	public String getDisclosedUserName() {
		return disclosedUserName;
	}

	public String getClientIp() {
		return clientIp;
	}

	public long getExecutionTimeDb() {
		return executionTimeDb;
	}

	public void dbCallCompleted(long executionTimeDb) {
		this.executionCountDb++;
		this.executionTimeDb += executionTimeDb;
	}

	public int getExecutionCountDb() {
		return executionCountDb;
	}

	public long getTimestampEnd() {
		return timestampEnd;
	}

	public String toJson() {
		return JsonUtils.toJson(this, "callStack");
	}

	@Override
	public String toString() {
		return toString(false);
	}

	public final String toString(boolean asciiArt) {
		return toString(asciiArt, true);
	}

	public String toString(boolean asciiArt, boolean callStack) {
		StringBuilder sb = new StringBuilder(3000);
		sb.append("id:     ").append(id).append('\n');
		sb.append("name:   ").append(getName()).append('\n');
		if (getParameters() != null) {
			sb.append("params: ").append(getParameters()).append('\n');
		}
		if (callStack) {
			appendCallStack(sb, asciiArt);
		}
		return sb.toString();
	}

	protected void appendCallStack(StringBuilder sb, boolean asciiArt) {
		if (getCallStack() != null) {
			sb.append(getCallStack().toString(asciiArt));
		}
	}

	public long getMeasurementStart() {
		return measurementStart;
	}

	@JsonAnyGetter
	public Map<String, Object> getCustomProperties() {
		return customProperties;
	}

	public void setCustomProperties(Map<String, Object> customProperties) {
		this.customProperties = customProperties;
	}

	/**
	 * Use this method to add a custom property to this request trace.
	 * <p/>
	 * You can use these properties in the Kibana dashboard.
	 *
	 * @param key   The key, which must not contain dots (.).
	 * @param value The value, which has to be serializable by jackson.
	 */
	@JsonAnySetter
	public void addCustomProperty(String key, Object value) {
		customProperties.put(key, value);
	}

	/**
	 * Adds an attribute to the request which can later be retrieved by {@link #getRequestAttribute(String)}
	 * <p/>
	 * The attributes won't be reported
	 * @param key
	 * @param value
	 */
	public void addRequestAttribute(String key, Object value) {
		requestAttributes.put(key, value);
	}

	public Object getRequestAttribute(String key) {
		return requestAttributes.get(key);
	}

	public String getUniqueVisitorId() {
		return uniqueVisitorId;
	}

	public void setUniqueVisitorId(String uniqueVisitorId) {
		this.uniqueVisitorId = uniqueVisitorId;
	}
}
