package de.isys.jawap.server.graphite

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Value

import javax.inject.Inject
import javax.inject.Named

@Named
class GraphiteClient {

	private String graphiteUrl

	@Inject
	public GraphiteClient(@Value("graphite.web.url") String graphiteWebUrl) {
		this.graphiteUrl = graphiteWebUrl
	}

	List<String> getApplications() { autocomplete('jawap.*') }

	List<String> getInstances(String application) { autocomplete("jawap.$application.*") }

	List<String> getHosts(String application, String instance) { autocomplete("jawap.$application.$instance.*") }

	List<String> getPlugins(String application) { autocomplete("jawap.$application.*.*.*") }

	private List<String> autocomplete(String query) {
		new JsonSlurper().parse("${graphiteUrl.toURL()}/metrics?query=$query".toURL())*.text
	}

}
