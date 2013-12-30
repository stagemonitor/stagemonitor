package de.isys.jawap.server.dashboard

import de.isys.jawap.server.graphite.GraphiteClient
import groovy.json.JsonSlurper
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import javax.annotation.Resource

import static de.isys.jawap.util.GraphiteEncoder.decodeForGraphite
import static groovyx.gpars.GParsPool.withPool
import static org.springframework.web.bind.annotation.RequestMethod.GET

@RestController
@RequestMapping("/graphitus")
class GraphitusController {

	@Resource
	GraphiteClient graphite

	@RequestMapping(value = "/configuration", method = GET)
	def getConfiguration() {
		return [
				graphiteUrl: "http://webstage-loadtest:8080",
				dashboardListUrl: "dashboards",
				dashboardUrlTemplate: 'dash?id=${dashboardId}',
				minimumRefresh: 60,
				timezones: ["US/Eastern", "US/Central", "US/Pacific", "Europe/London", "Europe/Berlin", "Israel"]
		]
	}

	@RequestMapping(value = "/dashboards", method = GET)
	def getDashboards() {
		withPool {
			List dashboards = graphite.applications.collectParallel { application ->
				graphite.getPlugins(application).collect { plugin ->
					[id: "${application}.$plugin".toString()]
				}
			}
			return [rows: dashboards.flatten()]
		}
	}

	@RequestMapping(value = "/dash", method = GET)
	def getDashboard(@RequestParam('id') String id) {
		def application = id.split(/\./)[0]
		def plugin = id.split(/\./)[1]

		return getDefaultSettings(application, plugin) +
				getDashboardPlugin(application, plugin)
	}

	def getDashboardPlugin(String application, String plugin) {
		String jawapPrefix = "jawap.\${application}.\${environment}.\${host}.${plugin}"
		new JsonSlurper().parseText('''
		{
			"data": [
				{
					"title": "Cpu utilisation",
					"target": "groupByNode([_jawapPrefix_].cpu.process.usage,\${group},'averageSeries'))",
					"yMax": "100"
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
				parameters: [
						"application": [
								"type": "dynamic",
								"query": "jawap.*",
								"index": 1,
								"showAll": false
						],
						"environment": [
								"type": "dynamic",
								"query": 'jawap.${application}.*',
								"index": 2,
								"showAll": true
						],
						"host": [
								"type": "dynamic",
								"query": 'jawap.${application}.${environment}.*',
								"index": 3,
								"showAll": true
						],
						"group": [
								"application": [
										"group": 1
								],
								"environment": [
										"group": 2
								],
								"host": [
										"group": 3
								]
						]
				]
		]
	}


	@RequestMapping("/requestTable")
	def getRequestTable() {
		[aaData: graphite.getRequestTable()]
	}

}
