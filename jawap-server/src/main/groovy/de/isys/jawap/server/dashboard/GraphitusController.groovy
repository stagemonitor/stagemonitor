package de.isys.jawap.server.dashboard

import de.isys.jawap.server.graphite.GraphiteClient
import org.springframework.core.io.ClassPathResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import javax.annotation.Resource
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

import static groovyx.gpars.GParsPool.withPool
import static org.springframework.web.bind.annotation.RequestMethod.GET

@RestController
@RequestMapping("/graphitus")
class GraphitusController {

	@Resource
	GraphiteClient graphite

	@PersistenceContext
	private EntityManager entityManager;

	@Resource
	private DashboardRepository dashboardRepository

	@RequestMapping(value = "/configuration", method = GET)
	def getConfiguration() {
		"jawap/config.json".loadJsonResource() + [graphiteUrl: graphite.graphiteUrl]
	}

	@RequestMapping(value = "/dashboards", method = GET)
	def getDashboards() {
//		def results = entityManager.createQuery("select name from Dashboard", String).resultList
		def results = ['jvm.overview', 'jvm.memory', 'request', 'server']
		return [rows: results.collect { [id: it] }]
	}

	@RequestMapping(value = "/dash", method = GET)
	def getDashboard(@RequestParam String id) {
		return "jawap/DefaultJawapDashboardSettings.json".loadJsonResource() +
				[title: id.split(/\./)*.capitalize().join(' ')] +
				getDashboardPlugin(id)
	}

	private def getDashboardPlugin(String id) {
		new ClassPathResource("jawap/plugins/${id}.json").getURL().text.json
//		dashboardRepository.findOne(id).content.json
	}

	@RequestMapping("/requestTable")
	def getRequestTable(@RequestParam application, @RequestParam environment, @RequestParam host,
						@RequestParam(required = false) from, @RequestParam(required = false) until) {
		[aaData: graphite.getRequestTable(application, environment, host, from, until)]
	}

}
