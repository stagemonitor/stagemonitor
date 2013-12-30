package de.isys.jawap.server.graphite

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties

import javax.inject.Named

import static de.isys.jawap.util.GraphiteEncoder.decodeForGraphite

@Named
@ConfigurationProperties(value = "jawap", ignoreInvalidFields = false)
class GraphiteClient {

	@Value('${jawap.graphiteUrl:http://webstage-loadtest:8080}')
	private String graphiteUrl

	List<String> getApplications() { autocomplete('jawap.*') }

	List<String> getInstances(String application) { autocomplete("jawap.$application.*") }

	List<String> getHosts(String application, String instance) { autocomplete("jawap.$application.$instance.*") }

	List<String> getPlugins(String application) { autocomplete("jawap.$application.*.*.*") }

	private List<String> autocomplete(String query) {
		new JsonSlurper().parseText("${graphiteUrl}/metrics?query=$query".toURL().text)*.text
	}


	List getRequestTable() {
		// for the structure of the json document, see http://graphite.readthedocs.org/en/0.9.x/render_api.html#json
		List json = new ObjectMapper().readValue("http://webstage-loadtest:8080/render?target=jawap.Spring_PetClinic.*.*.request.*.*&from=-1h&format=json".toURL(), List.class);
		json.findAll { entry -> ['m1_rate', 'max', 'mean', 'min', 'stddev', 'p50', 'p95' ].any{entry.target?.endsWith(it)}}
				.groupBy { decodeForGraphite(getRequestName(it.target)) } // group by request name
				.collect { requestName, metricsByRequestName ->
			[requestName] + getAggregatedMetrics(metricsByRequestName)
		}.findAll{ it[1] != null}
	}

	@CompileStatic private String getRequestName(String target) {
		target.split(/\./)[-2]
	}

	private def getAggregatedMetrics(metricsByRequestName) {
		metricsByRequestName.collect {
			def valueList = it.datapoints.collect { it[0] }.findAll { it != null }
			aggregateValues(getMetricType(it.target), valueList)?.round(2)
		}
	}

	@CompileStatic private String getMetricType(String target) {
		target[target.lastIndexOf('.')+1..<target.size()]
	}

	@CompileStatic
	private Double aggregateValues(String metricType, Collection<Number> values) {
		if (values.isEmpty())
			return null
		switch (metricType) {
			case 'max': return values.max().doubleValue()
			case 'min': return values.min().doubleValue()
			default: return (values.sum() as Double) / (double) values.size()
		}
	}

	public static void main(String[] args) {
		def client = new GraphiteClient()
		println client.getRequestTable()
	}

}
