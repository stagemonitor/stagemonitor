package org.stagemonitor.core;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.source.ConfigurationSource;
import org.stagemonitor.core.configuration.source.SimpleSource;

public class CorePluginTest {

	private CorePlugin corePlugin = new Configuration(
			Collections.singletonList(new CorePlugin()),
			Collections.<ConfigurationSource>singletonList(new SimpleSource("test")
					.add("stagemonitor.elasticsearch.url", "http://bla:1/,http://bla:2,http://bla:3")),
			null).getConfig(CorePlugin.class);

	@Test
	public void testCycleElasticsearchUrls() throws Exception {
		assertEquals("http://bla:1", corePlugin.getElasticsearchUrl());
		assertEquals("http://bla:2", corePlugin.getElasticsearchUrl());
		assertEquals("http://bla:3", corePlugin.getElasticsearchUrl());
		assertEquals("http://bla:1", corePlugin.getElasticsearchUrl());
		assertEquals("http://bla:2", corePlugin.getElasticsearchUrl());
		assertEquals("http://bla:3", corePlugin.getElasticsearchUrl());
	}
}
