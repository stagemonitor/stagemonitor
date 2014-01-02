package de.isys.jawap.server.graphite

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovyx.net.http.HTTPBuilder
import org.springframework.beans.factory.annotation.Value

import javax.inject.Inject
import javax.inject.Named

import static de.isys.jawap.util.GraphiteEncoder.decodeForGraphite

@Named
class GraphiteClient {

	String graphiteUrl
	private def http

	@Inject
	GraphiteClient(@Value('${jawap.graphiteUrl}') String graphiteUrl) {
		this.graphiteUrl = graphiteUrl
	}

	List<String> getApplications() { autocomplete('jawap.*') }

	List<String> getInstances(String application) { autocomplete("jawap.$application.*") }

	List<String> getHosts(String application, String instance) { autocomplete("jawap.$application.$instance.*") }

	List<String> getPlugins(String application) { autocomplete("jawap.$application.*.*.*") }

	private List<String> autocomplete(String query) {
		new JsonSlurper().parseText("${graphiteUrl}/metrics?query=$query".toURL().text)*.text
	}

	List<List> getRequestTable(String application, environment, host, String from, String until) {
		List<String> rawLines = new HTTPBuilder(graphiteUrl).get(
				path: "/render",
				query: [target: "jawap.${application}.${environment}.${host}.request.*.{m1_rate,max,mean,min,stddev,p50,p95}",
						from: from, until: until, format: 'raw'].findAll { it.value }).readLines()
		rawLines = rawLines.collect { it.replace('None,', '') }
		List<Map<String, Object>> structuredRawLines = getStructuredLines(rawLines)
		Map<String, List<Map>> groupedStructuredLines = structuredRawLines.groupBy { it.requestName }
		groupedStructuredLines.collect { String requestName, List<Map> structuredLines ->
			[requestName] + structuredLines.collect { aggregateValues(it.metricType, it.valueList)?.round(2) }
		}.findAll { it[1] != null }
	}

	@CompileStatic
	private List<Map<String, Object>> getStructuredLines(List<String> rawLines) {
		rawLines.collect { String raw ->
			String target = raw[0..<raw.indexOf(',')]
			[
					requestName: decodeForGraphite(target.split(/\./)[-2]),
					metricType: target[target.lastIndexOf('.') + 1..<target.size()],
					valueList: raw[raw.lastIndexOf('|') + 1..<raw.size()].split(/\,/).findAll { it != 'None' }.collect { it as Double }
			]
		}
	}

	@CompileStatic
	private Double aggregateValues(String metricType, Collection<? extends Number> values) {
		if (values.isEmpty())
			return null
		switch (metricType) {
			case 'max': return values.max().doubleValue()
			case 'min': return values.min().doubleValue()
			default: return (values.sum() as Double) / (double) values.size()
		}
	}

	public static void main(String[] args) {
		def client = new GraphiteClient('http://webstage-loadtest:8080')
		long start = System.currentTimeMillis();
		client.getRequestTable()
		println System.currentTimeMillis() - start;
		start = System.currentTimeMillis();
		client.getRequestTable()
		println System.currentTimeMillis() - start;
	}

}
