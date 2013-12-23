package de.isys.jawap.server.dashboard

import de.isys.jawap.server.graphite.GraphiteClient
import groovy.json.JsonSlurper
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import javax.annotation.Resource

import static de.isys.jawap.util.GraphiteEncoder.decodeForGraphite
import static org.springframework.web.bind.annotation.RequestMethod.GET

@RestController
@RequestMapping("/graphitus")
class GraphitusController {

	@Resource
	GraphiteClient graphite

	@RequestMapping(value = "/configuration", method = GET)
	def getConfiguration() {
		return [
				graphiteUrl: "http://webstage-loadtest",
				dashboardListUrl: "dashboards",
				dashboardUrlTemplate: 'dashboards/${dashboardId}',
				minimumRefresh: 60,
				timezones: ["US/Eastern", "US/Central", "US/Pacific", "Europe/London", "Israel"]
		]
	}

	@RequestMapping(value = "/dashboards", method = GET)
	def getDashboards() {
		def dashboards = []
		graphite.applications.each { application ->
			graphite.getPlugins(application).each { plugin ->
				dashboards << [id: "${application}.$plugin"]
			}
		}
		return [
				rows: [dashboards]
		]
	}

	@RequestMapping(value = "/dashboards/{id}", method = GET)
	def getDashboard(String id) {
		def application = id.split('.')[0]
		def plugin = id.split('.')[1]

		return getDefaultSettings(application, plugin) +
				getDashboardPlugin(application, plugin)
	}

	def getDashboardPlugin(String application, String plugin) {
		String jawapPrefix = "jawap.${application}.\${inst}.\${host}.${plugin}"
		new JsonSlurper().parseText('''
		{
			data: [
				{
					title: "Homepage",
					target: "groupByNode([_jawapPrefix_].GET_|.mean, 'Mean response time')"
				}
			]
		}'''.replace('[_jawapPrefix_]', jawapPrefix))
	}

	def getDefaultSettings(String application, String plugin) {
		return [
				title: plugin.capitalize(),
				columns: 2,
				timeBack: "6h",
				from: "",
				until: "",
				width: 700,
				height: 350,
				legend: true,
				refresh: true,
				refreshIntervalSeconds: 30,
				averageSeries: false,
				defaultLineWidth: 2,
				parameters: getJawapParameters(application)
		]
	}

	private def getJawapParameters(String application) {
		def instances = graphite.getInstances(application).collectEntries { [(decodeForGraphite(it)): [inst: it]] }
		def hosts = graphite.getHosts(application, '*').collectEntries { [(decodeForGraphite(it)): [host: it]] }
		return [
				Instance: [
						All: [inst: "*"]
				] + instances,
				Host: [
						All: [host: "*"]
				] + hosts
		]
	}


}
