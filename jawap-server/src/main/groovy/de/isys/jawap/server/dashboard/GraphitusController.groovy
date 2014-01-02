package de.isys.jawap.server.dashboard

import de.isys.jawap.server.graphite.GraphiteClient
import org.springframework.core.io.ClassPathResource
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import javax.annotation.Resource

import static groovyx.gpars.GParsPool.withPool
import static org.springframework.web.bind.annotation.RequestMethod.GET

@RestController
@RequestMapping("/graphitus")
class GraphitusController {

	@Resource
	GraphiteClient graphite

	@RequestMapping(value = "/configuration", method = GET)
	def getConfiguration() {
		"jawap/config.json".loadJsonResource() + [graphiteUrl: graphite.graphiteUrl]

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

		return "jawap/DefaultJawapDashboardSettings.json".loadJsonResource() +
				[title: plugin.capitalize()] +
				getDashboardPlugin(application, plugin)
	}

	private def getDashboardPlugin(String application, String plugin) {
		String jawapPrefix = "jawap.\${application}.\${environment}.\${host}.${plugin}"
		new ClassPathResource("jawap/plugins/${plugin}.json").getURL().text.replace('[_jawapPrefix_]', jawapPrefix).json
	}

	@RequestMapping("/requestTable")
	def getRequestTable(@RequestParam application,
						@RequestParam environment,
						@RequestParam host,
						@RequestParam(required = false) from,
						@RequestParam(required = false) until) {
		[aaData: graphite.getRequestTable(application, environment, host, from, until)]
	}

}
