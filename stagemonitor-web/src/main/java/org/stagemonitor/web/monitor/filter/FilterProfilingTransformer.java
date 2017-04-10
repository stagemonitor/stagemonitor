package org.stagemonitor.web.monitor.filter;

import static net.bytebuddy.matcher.ElementMatchers.isSubTypeOf;
import static net.bytebuddy.matcher.ElementMatchers.nameEndsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;

import javax.servlet.Filter;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.requestmonitor.profiler.ProfilingTransformer;

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
