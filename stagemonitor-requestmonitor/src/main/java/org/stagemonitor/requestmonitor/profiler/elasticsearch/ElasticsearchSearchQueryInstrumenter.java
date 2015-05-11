package org.stagemonitor.requestmonitor.profiler.elasticsearch;

import javassist.CtClass;
import javassist.CtMethod;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.core.util.StringUtils;

public class ElasticsearchSearchQueryInstrumenter extends StagemonitorJavassistInstrumenter {

	private static final String ACTION_REQUEST_BUILDER_CLASSNAME = "org/elasticsearch/action/search/SearchRequestBuilder";
	private final boolean elasticsearchOnClasspath;

	public ElasticsearchSearchQueryInstrumenter() {
		elasticsearchOnClasspath = ClassUtils.isPresent(ACTION_REQUEST_BUILDER_CLASSNAME.replace('/', '.'));
	}

	@Override
	public void transformClass(CtClass actionRequestBuilder, ClassLoader loader) throws Exception {
		final CtMethod doExecuteMethod = actionRequestBuilder.getDeclaredMethod("doExecute");
		actionRequestBuilder.getClassPool().importPackage("org.stagemonitor.requestmonitor.profiler.elasticsearch");
		actionRequestBuilder.getClassPool().importPackage("org.stagemonitor.requestmonitor.profiler");
		doExecuteMethod.insertBefore("Profiler.addCall(ElasticsearchSearchQueryInstrumenter.getSearchRequestAsString(this), 0L);");
	}

	@Override
	public boolean isIncluded(String className) {
		return elasticsearchOnClasspath && className.equals(ACTION_REQUEST_BUILDER_CLASSNAME);
	}

	public static String getSearchRequestAsString(SearchRequestBuilder searchRequestBuilder) {
		final SearchRequest request = searchRequestBuilder.request();

		String query = "POST /";
		if (request.indices().length > 0) {
			query += StringUtils.asCsv(request.indices())+ "/" ;
			if (request.types().length > 0) {
				query += StringUtils.asCsv(request.types()) + "/";
			}
		}
		query += "_search";
		query += getQueryParameters(request);
		query += "\n";
		query += searchRequestBuilder.toString();
		return query;
	}

	private static String getQueryParameters(SearchRequest request) {
		final StringBuilder queryParams = new StringBuilder();
		if (request.routing() != null) {
			queryParams.append("routing=").append(request.routing());
		}
		if (request.searchType() != SearchType.DEFAULT) {
			queryParams.append("search_type=").append(request.searchType().name().toLowerCase());
		}
		if (queryParams.length() > 0) {
			queryParams.insert(0, '?');
		}
		return queryParams.toString();
	}

}
