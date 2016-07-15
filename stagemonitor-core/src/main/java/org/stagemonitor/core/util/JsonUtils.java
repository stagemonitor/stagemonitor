package org.stagemonitor.core.util;

import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
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
		if (Double.parseDouble(System.getProperty("java.specification.version")) > 1.6) {
			AfterburnerModuleRegisterer.registerAfterburnerModule();
		}
		MAPPER.registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, false));
		MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}

	private JsonUtils() {
	}

	public static boolean versionsMatch(Version v1, Version v2) {
		return v1.getMajorVersion() == v2.getMajorVersion() &&
				v1.getMinorVersion() == v2.getMinorVersion() &&
				v1.getPatchLevel() == v2.getPatchLevel();
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

	public static <T> ObjectReader getObjectReader(Class<T> type) {
		final ObjectReader objectReader;
		if (getMapper().version().compareTo(new Version(2, 6, 0, null, "com.fasterxml.jackson.core", "jackson-databind")) >= 0) {
			objectReader = getMapper().readerFor(type);
		} else {
			objectReader = getMapper().reader(type);

		}
		return objectReader;
	}

	private static class AfterburnerModuleRegisterer {
		private static void registerAfterburnerModule() {
			/*
			 * Avoiding java.lang.NoSuchMethodError: com.fasterxml.jackson.databind.ser.BeanPropertyWriter.isUnwrapping()
			 * This happens, if the version of jackson databind less that the Afterburner version.
			 *
			 * One reason can be maven, because it resolves a version conflict with the nearest-wins strategy it is possible that
			 * jackson-module-afterburner is in a higher version that jackson-databind and jackson-core
			 *
			 * Another reason could be that a application server bundled version of jackson databind is used
			 */
			final AfterburnerModule afterburnerModule = new AfterburnerModule();
			if (versionsMatch(MAPPER.version(), afterburnerModule.version())) {
				MAPPER.registerModule(afterburnerModule);
			}
		}
	}
}
