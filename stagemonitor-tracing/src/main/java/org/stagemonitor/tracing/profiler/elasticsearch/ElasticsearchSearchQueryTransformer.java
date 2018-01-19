package org.stagemonitor.tracing.profiler.elasticsearch;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.core.util.VersionUtils;
import org.stagemonitor.tracing.profiler.Profiler;
import org.stagemonitor.util.StringUtils;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public class ElasticsearchSearchQueryTransformer extends StagemonitorByteBuddyTransformer {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchSearchQueryTransformer.class);

	private static final String ABSTRACT_CLIENT_CLASSNAME = "org.elasticsearch.client.support.AbstractClient";

	@Override
	public ElementMatcher.Junction<TypeDescription> getIncludeTypeMatcher() {
		return named(ABSTRACT_CLIENT_CLASSNAME);
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription> getMethodElementMatcher() {
		return named("execute").and(takesArguments(3));
	}

	@Advice.OnMethodEnter(inline = false)
	public static void addIOCall(@Advice.Argument(1) ActionRequest actionRequestBuilder) {
		if (actionRequestBuilder instanceof SearchRequest) {
			Profiler.addIOCall(ElasticsearchSearchQueryTransformer.getSearchRequestAsString((SearchRequest) actionRequestBuilder), 0L);
		}
	}

	@Override
	public boolean isActive() {
		// try to detect elasticsearch version <= 2
		Integer elasticsearchMajorVersion = VersionUtils.getMajorVersionFromPomProperties(SearchRequest.class, "org.elasticsearch", "elasticsearch");
		if (elasticsearchMajorVersion != null && elasticsearchMajorVersion <= 2) {
			logger.warn("Profiling Elasticsearch < 5 is no longer supported in the current stagemonitor version.");
			return false;
		}
		return ClassUtils.isPresent(ABSTRACT_CLIENT_CLASSNAME);
	}

	public static String getSearchRequestAsString(SearchRequest request) {

		String query = "POST /";
		if (request.indices().length > 0) {
			query += StringUtils.asCsv(request.indices()) + "/";
			if (request.types().length > 0) {
				query += StringUtils.asCsv(request.types()) + "/";
			}
		}
		query += "_search";
		query += getQueryParameters(request);
		query += "\n";
		query += request.source().toString();
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
