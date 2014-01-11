package de.isys.jawap.server.dashboard

import groovy.transform.CompileStatic
import groovyx.net.http.HTTPBuilder
import org.springframework.beans.factory.annotation.Value

import javax.inject.Inject
import javax.inject.Named
import java.text.MessageFormat

import static de.isys.jawap.util.GraphiteEncoder.decodeForGraphite

@Named
class GraphiteClient {

	String graphiteUrl
	/**
	 * Sometimes it takes some time until graphite returns results for all metric types.
	 * Providing default values makes sure, that all metric types are present so datatables doesn't get
	 * confused about missing coloums.
	 */
	private final defaultMetrics = [error: 0, m1_rate: null, max: null, mean: null, min: null, stddev: null, p50: null, p95: null]

	@Inject
	GraphiteClient(@Value('${jawap.graphiteUrl}') String graphiteUrl) {
		this.graphiteUrl = graphiteUrl
	}

	List<List> getRequestTable(String application, environment, host, String from, String until) {
		def targetPrefix = "jawap.${application}.${environment}.${host}.request"
		List<String> rawLines = new HTTPBuilder(graphiteUrl).get(
				path: "/render",
				query: [target: ["$targetPrefix.*.{m1_rate,max,mean,min,stddev,p50,p95}", "$targetPrefix.*.error.m1_rate"],
						from: from, until: until, format: 'raw'].findAll { it.value }).readLines()
		rawLines = rawLines.collect { it.replace('None,', '') }
		List<Map<String, Object>> structuredRawLines = getStructuredLines(rawLines)
		Map<String, List<Map>> groupedStructuredLines = structuredRawLines.groupBy { it.requestName }
		groupedStructuredLines.collect { String requestName, List<Map> structuredLines ->
			Map aggregatedMetrics = structuredLines.collectEntries {
				[(it.metricType): aggregateValues(it.metricType, it.valueList)?.round(2)]
			}
			[name: requestName] + defaultMetrics + aggregatedMetrics
		}.findAll { it.mean != null }.each {
			if (it.error != null && it.m1_rate)
				it.error = MessageFormat.format("{0,number,#.##%}", it.error / it.m1_rate)
		}
	}

	private List<Map<String, Object>> getStructuredLines(List<String> rawLines) {
		rawLines.collect { String raw ->
			String target = raw[0..<raw.indexOf(',')]
			String[] targetSplit = target.split(/\./)
			[
					requestName: decodeForGraphite(targetSplit[5]),
					metricType: targetSplit[6],
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

}
