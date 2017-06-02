package org.stagemonitor.web.servlet.filter;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.stagemonitor.tracing.profiler.ProfilingTransformer;

import javax.servlet.Filter;

import static net.bytebuddy.matcher.ElementMatchers.isSubTypeOf;
import static net.bytebuddy.matcher.ElementMatchers.nameEndsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class FilterProfilingTransformer extends ProfilingTransformer {

	@Override
	public ElementMatcher.Junction<TypeDescription> getExtraIncludeTypeMatcher() {
		return nameEndsWith("Filter").and(isSubTypeOf(Filter.class));
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription> getExtraMethodElementMatcher() {
		return named("doFilter");
	}

}
