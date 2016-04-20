package org.stagemonitor.web.monitor.servlet;

import static net.bytebuddy.matcher.ElementMatchers.isSubTypeOf;
import static net.bytebuddy.matcher.ElementMatchers.nameEndsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;

import javax.servlet.Servlet;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.requestmonitor.profiler.ProfilingTransformer;

public class ServletProfilingTransformer extends ProfilingTransformer {

	@Override
	public ElementMatcher.Junction<TypeDescription> getExtraIncludeTypeMatcher() {
		return nameEndsWith("Servlet").and(isSubTypeOf(Servlet.class));
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return named("service").or(named("render"));
	}

}
