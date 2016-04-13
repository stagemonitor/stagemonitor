package org.stagemonitor.requestmonitor.profiler.elasticsearch;

import static net.bytebuddy.matcher.ElementMatchers.named;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.profiler.Profiler;

public class ElasticsearchSearchQueryTransformer extends StagemonitorByteBuddyTransformer {

	private static final String SEARCH_REQUEST_BUILDER_CLASSNAME = "org.elasticsearch.action.search.SearchRequestBuilder";

	@Override
	public ElementMatcher.Junction<TypeDescription> getIncludeTypeMatcher() {
		return named(SEARCH_REQUEST_BUILDER_CLASSNAME);
	}

	@Override
	protected ElementMatcher.Junction<? super MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return named("beforeExecute").or(named("doExecute"));
	}

	@Advice.OnMethodEnter
	private static void addIOCall(@Advice.This SearchRequestBuilder searchRequestBuilder) {
		Profiler.addIOCall(ElasticsearchSearchQueryTransformer.getSearchRequestAsString(searchRequestBuilder), 0L);
	}

	@Override
	public boolean isActive() {
		return ClassUtils.isPresent(SEARCH_REQUEST_BUILDER_CLASSNAME);
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
