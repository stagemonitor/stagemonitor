package de.isys.jawap.server.profiler

import de.isys.jawap.entities.MeasurementSession
import de.isys.jawap.entities.web.HttpRequestContext
import de.isys.jawap.server.core.MeasurementSessionRepository
import de.isys.jawap.util.GraphiteEncoder
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import javax.annotation.Resource
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.persistence.criteria.*

import static de.isys.jawap.util.GraphiteEncoder.decodeForGraphite
import static org.springframework.web.bind.annotation.RequestMethod.GET
import static org.springframework.web.bind.annotation.RequestMethod.POST

@RestController
public class ExecutionContextController {

	@Resource
	private HttpRequestContextRepository httpRequestContextRepository;
	@PersistenceContext
	private EntityManager entityManager;

	@RequestMapping(value = "/executionContexts", method = GET)
	public List<HttpRequestContext> getAllHttpRequestContexts() {
		return httpRequestContextRepository.findAll();
	}

	@RequestMapping(value = "/executionContexts/{id}", method = GET, produces = MediaType.TEXT_PLAIN_VALUE)
	public String getHttpRequestContextsPlainText(@PathVariable Integer id) {
		httpRequestContextRepository.findOne(id).toString()
	}

	@RequestMapping(value = "/executionContexts/{id}", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public HttpRequestContext getHttpRequestContexts(@PathVariable Integer id) {
		httpRequestContextRepository.findOne(id)
	}

	@RequestMapping(value = "/executionContexts/search", method = GET)
	public def searchHttpRequestContexts(@RequestParam(defaultValue = '*') String application,
										  @RequestParam(defaultValue = '*') String environment,
										  @RequestParam(defaultValue = '*') String host,
										  @RequestParam String name) {
		application = decodeAndCheckNull(application)
		environment = decodeAndCheckNull(environment)
		host = decodeAndCheckNull(host)

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<HttpRequestContext> query = cb.createQuery(HttpRequestContext.class);
		Root<HttpRequestContext> httpRequestContext = query.from(HttpRequestContext.class);
		List<Predicate> restrictions = new ArrayList<Predicate>(3);
		restrictions.add(cb.equal(httpRequestContext.get("name"), name));
		if (application || host || environment) {
			final Join<HttpRequestContext, MeasurementSession> measurementSession = httpRequestContext.join("measurementSession");
			if (application) restrictions.add(cb.equal(measurementSession.get("applicationName"), application))
			if (host) restrictions.add(cb.equal(measurementSession.get("hostName"), host))
			if (environment) restrictions.add(cb.equal(measurementSession.get("instanceName"), environment))
		}
		query.where(restrictions.toArray(new Predicate[restrictions.size()]));
		query.orderBy(cb.desc(httpRequestContext.get("timestamp")))

		List<HttpRequestContext> searchResult = entityManager.createQuery(query).getResultList()
		def resultList =  searchResult.collect {
			String executionTime = "${(it.callStack.executionTime / 1_000_000d).round(2)} ms"
			String date = new Date(it.timestamp).format('yyyy/MM/dd hh:mm:ss')
			String url = "$it.method $it.url$it.queryParams"
			[it.id, executionTime, date, url, it.statusCode]
		}
		return [ aaData: resultList]
	}

	private String decodeAndCheckNull(String s) {
		s = decodeForGraphite(s)
		return s == '*' ? null : s
	}

	@RequestMapping(value = "/measurementSessions/{measurementSessionId}/executionContexts", method = POST)
	public void saveExecutionContext(@PathVariable Integer measurementSessionId, @RequestBody HttpRequestContext context) {
		context.setMeasurementSession(entityManager.find(MeasurementSession, measurementSessionId))
		httpRequestContextRepository.save(context);
	}
}
