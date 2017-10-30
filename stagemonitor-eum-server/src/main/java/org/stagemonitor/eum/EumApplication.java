package org.stagemonitor.eum;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.stagemonitor.web.servlet.initializer.ServletContainerInitializerUtil;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

@SpringBootApplication
@Configuration
public class EumApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return configureApplication(builder);
	}

	public static void main(String[] args) {
		configureApplication(new SpringApplicationBuilder()).run(args);
	}

	private static SpringApplicationBuilder configureApplication(SpringApplicationBuilder builder) {
		return builder.sources(EumApplication.class);
	}

	@Component
	public static class StagemonitorInitializer implements ServletContextInitializer {

		@Override
		public void onStartup(ServletContext servletContext) throws ServletException {
			// necessary for spring boot 2.0.0.M2 until stagemonitor supports it natively
			ServletContainerInitializerUtil.registerStagemonitorServletContainerInitializers(servletContext);
		}
	}

}
