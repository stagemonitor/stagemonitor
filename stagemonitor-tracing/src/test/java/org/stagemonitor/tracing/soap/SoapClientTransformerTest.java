package org.stagemonitor.tracing.soap;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.AbstractEmbeddedServerTest;
import org.stagemonitor.util.IOUtils;

import java.io.IOException;
import java.net.URL;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.handler.Handler;

import static org.assertj.core.api.Assertions.assertThat;

public class SoapClientTransformerTest extends AbstractEmbeddedServerTest {

	@Before
	public void setUpServer() throws Exception {
		Stagemonitor.init();
		startWithHandler(new AbstractHandler() {
			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				IOUtils.copy(IOUtils.getResourceAsStream("wsdl.xml"), response.getOutputStream());
				baseRequest.setHandled(true);
			}
		});
	}

	@Test
	public void test() throws Exception {
		QName serviceName = new QName("http://www.jboss.org/jbossas/quickstarts/wshelloworld/HelloWorld", "HelloWorldService");
		Service service = Service.create(new URL("http://localhost:" + getPort()), serviceName);

		HelloWorldService helloWorldService = service.getPort(HelloWorldService.class);
		assertThat(helloWorldService).isInstanceOf(BindingProvider.class);

		final BindingProvider bindingProvider = (BindingProvider) helloWorldService;
		boolean clientHandlerFound = false;
		for (Handler handler : bindingProvider.getBinding().getHandlerChain()) {
			if (handler instanceof TracingClientSOAPHandler) {
				clientHandlerFound = true;
			}
		}
		assertThat(clientHandlerFound).overridingErrorMessage("No %s found in %s",
				TracingClientSOAPHandler.class.getSimpleName(),
				bindingProvider.getBinding().getHandlerChain()).isTrue();
	}

	@WebService(targetNamespace = "http://www.jboss.org/jbossas/quickstarts/wshelloworld/HelloWorld")
	public interface HelloWorldService {
		@WebMethod
		String sayHello();
	}
}
