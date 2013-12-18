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
import javax.persistence.criteria.Root;
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
	@RequestMapping(method = GET)
	public List<HttpRequestContext> searchHttpRequestContexts(@RequestParam(required = false) String host,
															  @RequestParam(required = false) String instance,
															  @RequestParam String method, @RequestParam String name) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<HttpRequestContext> query = cb.createQuery(HttpRequestContext.class);
		Root<HttpRequestContext> root = query.from(HttpRequestContext.class);
		if (host != null || instance != null) {
			// TODO join
			Root<MeasurementSession> measurementSessionRoot = query.from(MeasurementSession.class);
			if (host != null) {
				query.where(cb.equal(measurementSessionRoot.get("hostName"), host));
			}
			if (instance != null) {
				query.where(cb.equal(measurementSessionRoot.get("instanceName"), instance));
			}
		}
		query.where(cb.equal(root.get("method"), method)); // TODO
		query.where(cb.equal(root.get("name"), name));
		return httpRequestContextRepository.findAll();
	}

	@RequestMapping(method = POST)
	public void saveExecutionContext(@RequestBody HttpRequestContext context) {
		httpRequestContextRepository.save(context);
	}
}
