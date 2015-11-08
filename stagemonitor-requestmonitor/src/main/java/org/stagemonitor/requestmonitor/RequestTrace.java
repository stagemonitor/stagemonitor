package org.stagemonitor.requestmonitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;

/**
 * A request trace is a data structure containing all the important information about a request.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestTrace {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final String id;
	private final GetNameCallback getNameCallback;
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
	private String parameter;
	@JsonProperty("measurement_start")
	private final long measurementStart;
	private final String application;
	private final String host;
	private final String instance;
	private String exceptionMessage;
	private String exceptionClass;
	private String exceptionStackTrace;
	private String username;
	private String clientIp;
	@JsonUnwrapped
	private Map<String, Object> customProperties = new HashMap<String, Object>();

	public RequestTrace(String requestId, GetNameCallback getNameCallback) {
		this.id = requestId != null ? requestId : UUID.randomUUID().toString();
		MeasurementSession measurementSession = Stagemonitor.getMeasurementSession();
		measurementStart = measurementSession.getStartTimestamp();
		application = measurementSession.getApplicationName();
		host = measurementSession.getHostName();
		instance = measurementSession.getInstanceName();
		this.getNameCallback = getNameCallback;
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

	public String getName() {
		if (name == null) {
			name = getNameCallback.getName();
		}
		return name;
	}

	/**
	 * Sets the name of the request (e.g. 'Show Item Detail'). It is only possible to set the name, if it has not
	 * already been set.
	 *
	 * @param name the name of the request
	 * @return <code>true</code>, if the name was successfully set, <code>false</code> if the name could not be set,
	 * because it has already been set.
	 */
	public boolean setName(String name) {
		if (this.name != null) {
			logger.warn("Name is already set ({}), can't overwrite it with '{}'.", this.name, name);
			return false;
		}
		this.name = name;
		return true;
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

	public String getParameter() {
		return parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
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
		if (e != null) {
			exceptionMessage = e.getMessage();
			exceptionClass = e.getClass().getCanonicalName();

			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			exceptionStackTrace = sw.getBuffer().toString();
		}
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
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
		if (getParameter() != null) {
			sb.append("params: ").append(getParameter()).append('\n');
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
	public void addCustomProperty(String key, Object value) {
		customProperties.put(key, value);
	}

	/**
	 * Determining the request name before the execution starts can be slow. For example
	 * org.springframework.web.servlet.HandlerMapping#getHandler takes a few milliseconds to return the MVC controller
	 * method that will handle the request.
	 * <p/>
	 * So the request name should be lazily initialized. If there is no lazy initialisation before the execution, the
	 * request name can may be determined in a more efficient way, for example by weaving an Aspect around
	 * HandlerMapping#getHandler that is called by Spring as a part of Spring's dispatching mechanism.
	 */
	public interface GetNameCallback {

		/**
		 * Gets the name of the request.
		 *
		 * @return the name of the request. For Example 'Show Item Details'.
		 */
		String getName();
	}
}
