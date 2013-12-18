package de.isys.jawap.server.profiler;

import de.isys.jawap.entities.profiler.CallStackElement;
import de.isys.jawap.entities.web.HttpRequestContext;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping("/executionContexts")
public class ExecutionContextController {
	@Resource
	private HttpRequestContextRepository httpRequestContextRepository;

	@PostConstruct
	void seedData() {
		HttpRequestContext httpRequestContext = new HttpRequestContext();
		httpRequestContext.setUrl("bla");

		CallStackElement callStack = new CallStackElement(null);
		httpRequestContext.setCallStack(callStack);
		callStack.setClassName("test");
		httpRequestContextRepository.save(httpRequestContext);
	}

	@RequestMapping(method = POST)
	public void saveExecutionContext(@RequestBody HttpRequestContext context) {
		httpRequestContextRepository.save(context);
	}
}
