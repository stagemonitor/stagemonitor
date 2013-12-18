package de.isys.jawap.server.profiler;

import de.isys.jawap.entities.web.HttpRequestContext;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping("/executionContexts")
public class ExecutionContextController {
	@Resource
	private HttpRequestContextRepository httpRequestContextRepository;

	@RequestMapping(method = GET)
	public List<HttpRequestContext> getAllHttpRequestContexts() {
		return httpRequestContextRepository.findAll();
	}

	@RequestMapping(method = POST)
	public void saveExecutionContext(@RequestBody HttpRequestContext context) {
		httpRequestContextRepository.save(context);
	}
}
