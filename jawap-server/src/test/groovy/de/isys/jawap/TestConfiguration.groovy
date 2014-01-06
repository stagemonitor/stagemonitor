package de.isys.jawap

import de.isys.jawap.server.core.MeasurementSessionRepository
import de.isys.jawap.server.profiler.HttpRequestContextRepository
import org.mockito.Mockito
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.PropertySource
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer

import javax.persistence.EntityManagerFactory

@Configuration
@ComponentScan(excludeFilters = @ComponentScan.Filter(value = Application, type = FilterType.ASSIGNABLE_TYPE))
@EnableAutoConfiguration(exclude = [HibernateJpaAutoConfiguration, JpaRepositoriesAutoConfiguration])
class TestConfiguration {

	@Bean
	HttpRequestContextRepository httpRequestContextRepository() {
		Mockito.mock(HttpRequestContextRepository)
	}
	@Bean
	MeasurementSessionRepository measurementSessionRepository() {
		Mockito.mock(MeasurementSessionRepository)
	}

	@Bean
	EntityManagerFactory entityManagerFactory() {
		Mockito.mock(EntityManagerFactory)
	}

}
