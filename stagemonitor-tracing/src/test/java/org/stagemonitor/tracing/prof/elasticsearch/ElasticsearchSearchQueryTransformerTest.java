package org.stagemonitor.tracing.prof.elasticsearch;

import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.AbstractElasticsearchTest;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.tracing.profiler.CallStackElement;
import org.stagemonitor.tracing.profiler.Profiler;

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
				"{\"query\":{\"match_all\":{\"boost\":1.0}}} ", total.getChildren().get(0).getSignature());
		Assert.assertEquals(total.toString(), "POST /_search?search_type=dfs_query_then_fetch\n" +
				"{\"query\":{\"match_all\":{\"boost\":1.0}}} ", total.getChildren().get(1).getSignature());
	}
}
