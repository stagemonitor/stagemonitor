package org.stagemonitor.collector.core.rest;

import org.stagemonitor.entities.MeasurementSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MeasurementSessionRestClient {
	private final Log logger = LogFactory.getLog(getClass());

	private String serverUrl;

	public MeasurementSessionRestClient(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	public String saveMeasurementSession(MeasurementSession measurementSession) {
		if (serverUrl != null && !serverUrl.isEmpty()) {
			try {
				return RestClient.sendAsJson(serverUrl + "/measurementSessions", "POST", measurementSession).
						getHeaderField("Location");
			} catch (RuntimeException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return null;
	}

	public void updateMeasurementSession(MeasurementSession measurementSession, String location) {
		try {
			if (serverUrl != null && !serverUrl.isEmpty() && location != null) {
				String url;
				if (location.startsWith("/")) {
					url = serverUrl + location;
				} else {
					url = location;
				}
				RestClient.sendAsJson(url, "PUT", measurementSession);
			}
		} catch (RuntimeException e) {
			logger.error(e.getMessage(), e);
		}
	}
}
