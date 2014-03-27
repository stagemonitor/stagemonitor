package org.stagemonitor.server.util

import groovy.json.JsonSlurper
import org.codehaus.jackson.map.ObjectMapper
import org.springframework.core.io.ClassPathResource


class ExtensionMethods {

	public static def getJson(String self) {
		new ObjectMapper().readValue(self, Object.class)
	}

	public static def loadJsonResource(String location) {
		getJson(new ClassPathResource(location).getURL().text)
	}
}
