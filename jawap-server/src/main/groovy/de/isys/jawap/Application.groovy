package de.isys.jawap;

import de.isys.jawap.entities.MeasurementSession;
import de.isys.jawap.entities.profiler.CallStackElement;
import de.isys.jawap.entities.web.HttpRequestContext;
import de.isys.jawap.server.profiler.HttpRequestContextRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.SpringBootServletInitializer
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@ComponentScan
@Configuration
@EnableAutoConfiguration
@EnableTransactionManagement
@PropertySource("classpath:application.properties")
public class Application extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(Application.class);
	}

	@Resource
	private HttpRequestContextRepository httpRequestContextRepository;

	@PostConstruct
	void seedData() {
		MeasurementSession measurementSession = new MeasurementSession();
		measurementSession.setHostName("localhorst");
		measurementSession.setApplicationName("jawap");
		measurementSession.setInstanceName("test");

		HttpRequestContext httpRequestContext = new HttpRequestContext();
		httpRequestContext.setMeasurementSession(measurementSession);
		httpRequestContext.setName("bla");
		httpRequestContext.setUrl("bla");

		CallStackElement callStack = new CallStackElement(null);
		httpRequestContext.setCallStack(callStack);
		callStack.setClassName("test");
		httpRequestContextRepository.save(httpRequestContext);
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
