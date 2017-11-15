package org.stagemonitor.core.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.source.AbstractConfigurationSource;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.http.HttpRequest;
import org.stagemonitor.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

/**
 * A configuration source to pull stagemonitor configuration from a Spring Cloud Config Server. <p> A simple http GET
 * call is made to retrieve a list of key/value pairs by calling the /{application}-{profile}.properties end point.<br>
 * Multiple property sources are already considered on the server side, so a flat key/value list (separated by line
 * breaks) is returned from the server.<p> The config source can handle additions, changes and removals for
 * properties.<br> The configuration map is only updated when it got a valid response from the server (i.e. 200, not
 * exception, valid response body). On failure the cached properties are still available.<br> Note that
 * {stagemonitor.applicationName} is required with a non default value for this to work, as the application name is used
 * to pull the appropriate configuration. <p> Example as returned from the server when requesting *.properties:
 * <pre> {@code stagemonitor.active: true
 * stagemonitor.instrument.include: my.domain
 * stagemonitor.web.paths.excluded: /some/path
 * }</pre>
 * <br> The SpringCloudConfigConfigurationSource is controlled via the following properties. See {@code CorePlugin} for
 * their corresponding detailed description
 * <pre> {@code stagemonitor.configuration.springcloud.enabled=(true|false)
 * stagemonitor.configuration.springcloud.address=<configserveraddress>
 * stagemonitor.applicationName=<applicationName>
 * stagemonitor.configuration.springcloud.configurationSourceProfiles=<profile>[, <profile>]...
 * stagemonitor.configuration.springcloud.deactivateStagemonitorIfConfigServerIsDown=(true|false)
 * }</pre>
 */
public class SpringCloudConfigConfigurationSource extends AbstractConfigurationSource {
	private static final Logger logger = LoggerFactory.getLogger(SpringCloudConfigConfigurationSource.class);

	// The default profile for a Spring Environment if no specific Spring profile is set
	public static final String DEFAULT_PROFILE = "default";
	private final String profile;
	private final String applicationName;

	private Properties properties;
	private HttpClient httpClient;
	private String configAddress;


	/**
	 * Creates an ConfigServerConfigurationSource instance and fetches the configuration initially via a http GET
	 *
	 * @param configServerAddress Http or https address of the config server
	 * @param applicationName     Name of the application to fetch config for
	 */
	public SpringCloudConfigConfigurationSource(String configServerAddress, String applicationName) {
		this(new HttpClient(), configServerAddress, applicationName, DEFAULT_PROFILE);
	}

	/**
	 * Creates an ConfigServerConfigurationSource instance and fetches the configuration initially via a http GET.<br>
	 * Uses the DEFAULT_PROFILE
	 *
	 * @param httpClient          Instance of HttpClient
	 * @param configServerAddress Http or https address of the config server
	 * @param applicationName     Name of the application to fetch config for
	 */
	public SpringCloudConfigConfigurationSource(HttpClient httpClient, String configServerAddress, String applicationName) {
		this(httpClient, configServerAddress, applicationName, DEFAULT_PROFILE);
	}


	/**
	 * Creates an ConfigServerConfigurationSource instance and fetches the configuration initially via a http GET
	 *
	 * @param httpClient          Instance of HttpClient
	 * @param configServerAddress Http or https address of the config server
	 * @param applicationName     Name of the application to fetch config for
	 * @param profile             Name of profile to be used. If empty or null "default" will be used
	 */
	public SpringCloudConfigConfigurationSource(HttpClient httpClient, String configServerAddress, String applicationName, String profile) {

		if (StringUtils.isEmpty(configServerAddress) || !configServerAddress.startsWith("http")) {
			throw new IllegalArgumentException("Invalid config server address: " + configServerAddress);
		}

		if (StringUtils.isEmpty(applicationName)) {
			throw new IllegalArgumentException("The application name must not be empty.");
		}

		this.httpClient = httpClient;
		this.properties = new Properties();
		this.profile = profile;
		this.applicationName = applicationName;

		this.configAddress = getFullQualifiedConfigUrl(configServerAddress, applicationName, profile);
		reload();
	}


	/**
	 * Return the property value for a give key
	 *
	 * @param key the property key
	 * @return value of property, or null
	 */
	@Override
	public String getValue(String key) {
		return properties.getProperty(key);
	}


	/**
	 * Returns the config address of this config source
	 *
	 * @return full config address including service and profile
	 */
	@Override
	public String getName() {
		return configAddress;
	}


	public String getProfile() {
		return profile;
	}


	public String getApplicationName() {
		return applicationName;
	}


	@Override
	public void reload() {

		logger.debug("Requesting configuration from: " + configAddress);

		httpClient.send("GET", configAddress, new HashMap<String, String>(), null, new HttpClient.ResponseHandler<Void>() {
			@Override
			public Void handleResponse(HttpRequest<?> httpRequest, InputStream is, Integer statusCode, IOException e) throws IOException {
				if (e != null || statusCode != 200 || is == null) {
					logger.warn("Couldn't GET configuration. " + configAddress + " returned " + statusCode);
					return null;
				}

				logger.debug("reloading ConfigServerConfigurationSource with: " + properties);
				properties.clear();
				properties.load(is);
				return null;
			}
		});
	}

	/**
	 * Builds the effective url for the config for a given application and profile<br> e.g.
	 * https://localhost/myapp-prod.properties
	 *
	 * @param address         Address of the Spring Cloud Config server
	 * @param applicationName Name of application to get config for
	 * @param profile         Profile to get config for
	 * @return Full qualified configuration url
	 */
	public static String getFullQualifiedConfigUrl(String address, String applicationName, String profile) {
		return String.format("%s/%s-%s.properties", address, applicationName, profile);
	}
}
