package de.isys.jawap.server.dashboard

import de.isys.jawap.util.GraphiteEncoder
import org.springframework.core.io.ClassPathResource
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import javax.annotation.Resource
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

import static de.isys.jawap.util.GraphiteEncoder.encodeForGraphite
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

	@RequestMapping(value = "/config.json", method = GET)
	def getConfiguration() {
		"jawap/config.json".loadJsonResource() + [graphiteUrl: graphite.graphiteUrl]
	}

	@RequestMapping(value = "/dashboard.html", method = GET)
	def getDashboardHtml() {
		new ClassPathResource("static/graphitus/dashboard.html").getURL().text.replace('<script type="text/javascript" src="js/histogram.js"></script>',
				"""<script type="text/javascript" src="js/histogram.js"></script>
					<script type="text/javascript" src="//cdnjs.cloudflare.com/ajax/libs/datatables/1.9.4/jquery.dataTables.min.js"></script>
					<script type="text/javascript" src="../js/datatables-extensions.js"></script>
					<script type="text/javascript" src="../js/requestTable.js"></script>
					<link href="//cdnjs.cloudflare.com/ajax/libs/datatables/1.9.4/css/demo_table.css" rel="stylesheet">
				""")
	}

	// TODO strore dashboards in db
	@RequestMapping(value = "/dashboards", method = GET)
	def getDashboards() {
//		def results = entityManager.createQuery("select name from Dashboard", String).resultList
		def results = ['request', 'server', 'sql', 'jvm.overview', 'jvm.memory']
		return [rows: results.collect { [id: it] }]
	}

	@RequestMapping(value = "/dash", method = GET)
	def getDashboard(@RequestParam String id) {
		def defaultSettings = "jawap/DefaultJawapDashboardSettings.json".loadJsonResource()
		def dashboard =  getDashboardPlugin(id)
		if (!dashboard.groupBy) defaultSettings.parameters.remove('group-by')
		return defaultSettings + [title: id.split(/\./)*.capitalize().join(' ')] + dashboard
	}

	private def getDashboardPlugin(String id) {
		new ClassPathResource("jawap/plugins/${id}.json").getURL().text.json
//		dashboardRepository.findOne(id).content.json
	}

	@RequestMapping("/requestTable")
	def getRequestTable(@RequestParam application, @RequestParam instance, @RequestParam host,
						@RequestParam(required = false) from, @RequestParam(required = false) until) {
		[aaData: graphite.getRequestTable(application, instance, host, from, until)]
	}

}
