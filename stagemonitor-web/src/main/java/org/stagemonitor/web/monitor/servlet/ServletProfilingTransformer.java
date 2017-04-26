package org.stagemonitor.web.monitor.servlet;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.stagemonitor.tracing.profiler.ProfilingTransformer;

import javax.servlet.Servlet;

import static net.bytebuddy.matcher.ElementMatchers.isSubTypeOf;
import static net.bytebuddy.matcher.ElementMatchers.nameEndsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class ServletProfilingTransformer extends ProfilingTransformer {

	@Override
	public ElementMatcher.Junction<TypeDescription> getExtraIncludeTypeMatcher() {
		return nameEndsWith("Servlet").and(isSubTypeOf(Servlet.class));
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription> getExtraMethodElementMatcher() {
		return named("service").or(named("render"));
	}

}
