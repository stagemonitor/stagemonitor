package org.stagemonitor.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

public class JsonUtils {

	private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

	private static final ObjectMapper MAPPER = new ObjectMapper();

	static {
		MAPPER.registerModule(new AfterburnerModule());
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
}
