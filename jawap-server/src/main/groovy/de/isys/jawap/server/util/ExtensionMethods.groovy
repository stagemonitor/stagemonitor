package de.isys.jawap.server.util

import groovy.json.JsonSlurper
import org.springframework.core.io.ClassPathResource


class ExtensionMethods {

	public static def getJson(String self) {
		new JsonSlurper().parseText(self)
	}

	public static def loadJsonResource(String location) {
		getJson(new ClassPathResource(location).getURL().text)
	}
}
