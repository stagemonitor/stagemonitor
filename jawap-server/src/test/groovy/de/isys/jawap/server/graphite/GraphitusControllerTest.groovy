package de.isys.jawap.server.graphite

import de.isys.jawap.TestConfiguration
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.context.WebApplicationContext

import javax.inject.Inject

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup

@RunWith(SpringJUnit4ClassRunner)
@WebAppConfiguration
@ContextConfiguration(classes = [TestConfiguration])
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
		def json = mvc.perform(get("/graphitus/configuration"))
				.andExpect(status().isOk())
				.andReturn().response.contentAsString.json

		assert json.dashboardUrlTemplate
		assert json.graphiteUrl
	}

	@Test
	void testGetDashboard() {
		def json = mvc.perform(get("/graphitus/dash").param("id", "test.jvm"))
				.andExpect(status().isOk())
				.andReturn().response.contentAsString.json
		println json
		assert json.title == "Jvm"
		assert json.data.first().target == 'groupByNode(jawap.${application}.${environment}.${host}.jvm.cpu.process.usage,${group},\'averageSeries\'))'
	}

}
