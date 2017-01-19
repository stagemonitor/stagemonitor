package org.stagemonitor.requestmonitor.prof.elasticsearch;

import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.AbstractElasticsearchTest;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;

public class ElasticsearchSearchQueryTransformerTest extends AbstractElasticsearchTest {

	@BeforeClass
	public static void attachProfiler() {
		Stagemonitor.init();
	}

	@Test
	public void testCollectElasticsearchQueries() throws Exception {
		CallStackElement total = Profiler.activateProfiling("total");
		client.prepareSearch().setQuery(QueryBuilders.matchAllQuery()).get();
		client.prepareSearch().setQuery(QueryBuilders.matchAllQuery()).setSearchType(SearchType.DFS_QUERY_THEN_FETCH).get();
		Profiler.stop();
		Assert.assertEquals(total.toString(), "POST /_search\n" +
				"{\n" +
				"  \"query\" : {\n" +
				"    \"match_all\" : {\n" +
				"      \"boost\" : 1.0\n" +
				"    }\n" +
				"  },\n" +
				"  \"ext\" : { }\n" +
				"} ", total.getChildren().get(0).getSignature());
		Assert.assertEquals(total.toString(), "POST /_search?search_type=dfs_query_then_fetch\n" +
				"{\n" +
				"  \"query\" : {\n" +
				"    \"match_all\" : {\n" +
				"      \"boost\" : 1.0\n" +
				"    }\n" +
				"  },\n" +
				"  \"ext\" : { }\n" +
				"} ", total.getChildren().get(1).getSignature());
	}
}
