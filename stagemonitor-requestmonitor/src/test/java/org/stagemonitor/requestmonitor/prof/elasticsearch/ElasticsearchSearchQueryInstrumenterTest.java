package org.stagemonitor.requestmonitor.prof.elasticsearch;

import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.configuration.AbstractElasticsearchTest;
import org.stagemonitor.core.instrument.MainStagemonitorClassFileTransformer;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;

public class ElasticsearchSearchQueryInstrumenterTest extends AbstractElasticsearchTest {

	@BeforeClass
	public static void attachProfiler() {
		MainStagemonitorClassFileTransformer.performRuntimeAttachment();
	}

	@Test
	public void testCollectElasticsearchQueries() throws Exception {
		CallStackElement total = Profiler.activateProfiling("total");
		client.prepareSearch().setQuery(QueryBuilders.matchAllQuery()).get();
		client.prepareSearch().setQuery(QueryBuilders.matchAllQuery()).setSearchType(SearchType.COUNT).get();
		Profiler.stop();
		Assert.assertEquals("POST /_search\n" +
				"{\n" +
				"  \"query\" : {\n" +
				"    \"match_all\" : { }\n" +
				"  }\n" +
				"}", total.getChildren().get(0).getSignature());
		Assert.assertEquals("POST /_search?search_type=count\n" +
				"{\n" +
				"  \"query\" : {\n" +
				"    \"match_all\" : { }\n" +
				"  }\n" +
				"}", total.getChildren().get(1).getSignature());
	}
}
