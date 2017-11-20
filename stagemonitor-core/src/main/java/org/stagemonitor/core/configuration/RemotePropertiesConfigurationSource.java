package org.stagemonitor.core.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.source.AbstractConfigurationSource;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.http.HttpRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;

/**
 * A configuration source to pull stagemonitor configuration from a remote property configuration server. <p> A simple http GET
 * call is made to retrieve a list of key/value pairs by calling the specified configUrl.<br>
 * The properties are expected in a simple line oriented format with key/value pairs. For more information on the format,
 * see <a href="https://docs.oracle.com/javase/7/docs/api/java/util/Properties.html#load(java.io.Reader">java.util.Properties.load(java.io.Reader)</a>)
 * The config source can handle additions, changes and removals for
 * properties.<br> The configuration map is only updated when it got a valid response from the server (i.e. 200, not
 * exception, valid response body). On failure the cached properties are still available.<br> Note that
 * {stagemonitor.applicationName} is required with a non default value for this to work, as the application name is used
 * to pull the appropriate configuration. <p> Example as returned from the server when requesting *.properties:
 * <pre> {@code stagemonitor.active: true
 * stagemonitor.instrument.include: my.domain
 * stagemonitor.web.paths.excluded: /some/path
 * }</pre>
 * The RemotePropertiesConfigurationSource is controlled via the following properties. See {@code CorePlugin} for
 * their corresponding detailed description
 * <pre>
 * stagemonitor.configuration.remoteproperties.urls=configUrl[, configUrl]...
 * stagemonitor.configuration.remoteproperties.deactivateStagemonitorIfConfigServerIsDown=(true|false)
 * </pre>
 */
public class RemotePropertiesConfigurationSource extends AbstractConfigurationSource {
	private static final Logger logger = LoggerFactory.getLogger(RemotePropertiesConfigurationSource.class);

	private Properties properties;
	private HttpClient httpClient;
	private URL configUrl;

	/**
	 * Creates an RemotePropertiesConfigurationSource instance and fetches the configuration initially via a http GET
	 *
	 * @param configUrl Http or https address of the config server
	 */
	public RemotePropertiesConfigurationSource(URL configUrl) {
		this(new HttpClient(), configUrl);
	}

	/**
	 * Creates an RemotePropertiesConfigurationSource instance and fetches the configuration initially via a http GET
	 *  @param httpClient Instance of HttpClient
	 * @param configUrl  Http or https address of the config server
	 */
	public RemotePropertiesConfigurationSource(HttpClient httpClient, URL configUrl) {

		this.httpClient = httpClient;
		this.properties = new Properties();
		this.configUrl = configUrl;

		reload();
	}

	/**
	 * Return the property value for a given key
	 *
	 * @param key the property key
	 * @return value of property, or null
	 */
	@Override
	public String getValue(String key) {
		return properties.getProperty(key);
	}

	/**
	 * Returns the config url of this config source
	 *
	 * @return full config url
	 */
	@Override
	public String getName() {
		return configUrl.toExternalForm();
	}

	@Override
	public void reload() {

		logger.debug("Requesting configuration from: " + configUrl.toExternalForm());

		httpClient.send("GET", configUrl.toExternalForm(), new HashMap<String, String>(), null, new HttpClient.ResponseHandler<Void>() {
			@Override
			public Void handleResponse(HttpRequest<?> httpRequest, InputStream is, Integer statusCode, IOException e) throws IOException {
				if (e != null || statusCode != 200 || is == null) {
					logger.warn("Couldn't GET configuration. " + configUrl.toExternalForm() + " returned " + statusCode);
					return null;
				}

				logger.debug("Reloading RemotePropertiesConfigurationSource with: " + properties);
				final Properties newProperties = new Properties();
				newProperties.load(is);
				RemotePropertiesConfigurationSource.this.properties = newProperties;
				return null;
			}
		});
	}
}
