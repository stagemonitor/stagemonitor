package org.stagemonitor.vertx.transformers;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.vertx.VertxPlugin;
import org.stagemonitor.vertx.wrappers.RouteMonitoringHandler;

import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.Route;
import io.vertx.rxjava.ext.web.RoutingContext;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public class RouteHandlerTransformer extends StagemonitorByteBuddyTransformer {

	public static Logger logger = LoggerFactory.getLogger(RouteHandlerTransformer.class);

	private static VertxPlugin vertxPlugin = Stagemonitor.getPlugin(VertxPlugin.class);

	@Advice.OnMethodEnter
	public static void wrapHandler(@Advice.Argument(value = 0, readOnly = false) Handler<RoutingContext> handler) {
		handler = new RouteMonitoringHandler(handler);
	}

	@Override
	protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
		return RouteHandlerTransformer.class;
	}

	@Override
	protected ElementMatcher.Junction<TypeDescription> getNarrowTypesMatcher() {
		return named(vertxPlugin.getWebRouteImplementation());
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return named("handler")
				.and(takesArguments(Handler.class))
				.and(returns(Route.class));
	}
}
