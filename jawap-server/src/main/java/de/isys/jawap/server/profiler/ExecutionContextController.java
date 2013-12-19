package de.isys.jawap.server.profiler;

import de.isys.jawap.entities.MeasurementSession;
import de.isys.jawap.entities.web.HttpRequestContext;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping("/executionContexts")
public class ExecutionContextController {
	@Resource
	private HttpRequestContextRepository httpRequestContextRepository;
	@PersistenceContext
	private EntityManager entityManager;

	@RequestMapping(method = GET)
	public List<HttpRequestContext> getAllHttpRequestContexts() {
		return httpRequestContextRepository.findAll();
	}

	@RequestMapping(value = "/search", method = GET)
	public List<HttpRequestContext> searchHttpRequestContexts(@RequestParam(required = false) String host,
															  @RequestParam(required = false) String instance,
															  /*@RequestParam String method,*/ @RequestParam String name) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<HttpRequestContext> query = cb.createQuery(HttpRequestContext.class);
		Root<HttpRequestContext> httpRequestContext = query.from(HttpRequestContext.class);
		List<Predicate> restrictions = new ArrayList<Predicate>(3);
		restrictions.add(cb.equal(httpRequestContext.get("name"), name));
		if (host != null || instance != null) {
			final Join<HttpRequestContext, MeasurementSession> measurementSession = httpRequestContext.join("measurementSession");
			if (host != null) {
				restrictions.add(cb.equal(measurementSession.get("hostName"), host));
			}
			if (instance != null) {
				restrictions.add(cb.equal(measurementSession.get("instanceName"), instance));
			}
		}
		query.where(restrictions.toArray(new Predicate[restrictions.size()]));
		return entityManager.createQuery(query).getResultList();
	}

	@RequestMapping(method = POST)
	public void saveExecutionContext(@RequestBody HttpRequestContext context) {
		httpRequestContextRepository.save(context);
	}
}
