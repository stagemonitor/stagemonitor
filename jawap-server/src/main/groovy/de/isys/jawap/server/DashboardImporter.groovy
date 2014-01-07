package de.isys.jawap.server

import de.isys.jawap.server.dashboard.Dashboard
import de.isys.jawap.server.dashboard.DashboardRepository
import org.springframework.core.io.ClassPathResource
import org.springframework.transaction.annotation.Transactional

import javax.annotation.PostConstruct
import javax.inject.Inject
import javax.inject.Named
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@Named
class DashboardImporter {
	@PersistenceContext
	private EntityManager em
	@Inject
	private DashboardRepository dashboardRepository

	@PostConstruct
	@Transactional
	importDashboards() {
		["jvm", "request"].each {
			if (em.createQuery("select count(*) from Dashboard where id='${it}'").singleResult < 1) {
				dashboardRepository.save(new Dashboard(name: it, content: new ClassPathResource("jawap/plugins/${it}.json").URL.text));
			}
		}
	}
}
