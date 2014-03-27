package org.stagemonitor.server.graphite

import org.stagemonitor.server.WebAppJpaTest
import org.junit.Before
import org.junit.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.context.WebApplicationContext

import javax.inject.Inject

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup

@WebAppJpaTest
class GraphitusControllerTest {

	@Inject
	private WebApplicationContext wac;

	private MockMvc mvc;

	@Before
	public void setup() {
		this.mvc = webAppContextSetup(this.wac).build();
	}

	@Test
	void testGetConfiguration() {
		def json = mvc.perform(get("/graphitus/config.json"))
				.andExpect(status().isOk())
				.andReturn().response.contentAsString.json

		assert json.dashboardUrlTemplate
		assert json.graphiteUrl
	}

	@Test
	void testGetDashboard() {
		def json = mvc.perform(get("/graphitus/dash").param("id", "jvm.overview"))
				.andExpect(status().isOk())
				.andReturn().response.contentAsString.json
		println json
		assert json.title == "Jvm Overview"
		assert json.data.first().target.contains('stagemonitor.${application}.${instance}.${host}')

	}

}
