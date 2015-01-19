package org.stagemonitor.core.util;

import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class JsonUtils {

	private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

	private static final ObjectMapper MAPPER = new ObjectMapper();

	static {
		// Avoiding java.lang.NoSuchMethodError: com.fasterxml.jackson.databind.ser.BeanPropertyWriter.isUnwrapping()
		// This happens, if the version of jackson databind is < 2.3.0.
		// Because maven resolves a version conflict with the nearest-wins strategy it is possible that
		// jackson-module-afterburner is in a higher version that jackson-databind and jackson-core
		if (MAPPER.version().compareTo(new Version(2, 3, 0, null, "com.fasterxml.jackson.core", "jackson-databind")) >= 0) {
			MAPPER.registerModule(new AfterburnerModule());
		}
		MAPPER.registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, false));
	}

	public static String toJson(Object o) {
		if (o == null) {
			return null;
		}
		try {
			return MAPPER.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	public static String toJson(Object o, String exclude) {
		final ObjectNode jsonNode = MAPPER.valueToTree(o);
		jsonNode.remove(exclude);
		return jsonNode.toString();
	}

	public static void writeJsonToOutputStream(Object o, OutputStream os) throws IOException {
		MAPPER.writeValue(os, o);
	}

	public static ObjectMapper getMapper() {
		return MAPPER;
	}

	public static ObjectNode toObjectNode(Object o) {
		return MAPPER.valueToTree(o);
	}
}
