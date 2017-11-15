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

	private Properties properties;
	private HttpClient httpClient;
	private String configUrl;


	/**
	 * Creates an ConfigServerConfigurationSource instance and fetches the configuration initially via a http GET
	 *
	 * @param configUrl Http or https address of the config server
	 */
	public SpringCloudConfigConfigurationSource(String configUrl) {
		this(new HttpClient(), configUrl);
	}

	/**
	 * Creates an ConfigServerConfigurationSource instance and fetches the configuration initially via a http GET
	 *
	 * @param httpClient Instance of HttpClient
	 * @param configUrl  Http or https address of the config server
	 */
	public SpringCloudConfigConfigurationSource(HttpClient httpClient, String configUrl) {

		if (StringUtils.isEmpty(configUrl) || !configUrl.startsWith("http")) {
			throw new IllegalArgumentException("Invalid config server address: " + configUrl);
		}

		this.httpClient = httpClient;
		this.properties = new Properties();
		this.configUrl = configUrl;

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
		return configUrl;
	}

	@Override
	public void reload() {

		logger.debug("Requesting configuration from: " + configUrl);

		httpClient.send("GET", configUrl, new HashMap<String, String>(), null, new HttpClient.ResponseHandler<Void>() {
			@Override
			public Void handleResponse(HttpRequest<?> httpRequest, InputStream is, Integer statusCode, IOException e) throws IOException {
				if (e != null || statusCode != 200 || is == null) {
					logger.warn("Couldn't GET configuration. " + configUrl + " returned " + statusCode);
					return null;
				}

				logger.debug("reloading ConfigServerConfigurationSource with: " + properties);
				properties.clear();
				properties.load(is);
				return null;
			}
		});
	}
}
