package org.stagemonitor.vertx.transformers;

import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.Route;
import io.vertx.rxjava.ext.web.RoutingContext;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.vertx.wrappers.RequestMonitoringHandler;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class RouteHandlerTransformer extends StagemonitorByteBuddyTransformer {

    public static Logger logger = LoggerFactory.getLogger(RouteHandlerTransformer.class);

    @Override
    protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
        return RouteHandlerTransformer.class;
    }

    @Override
    protected ElementMatcher.Junction<TypeDescription> getNarrowTypesMatcher() {
        return named("io.vertx.rxjava.ext.web.Route");
    }

    @Override
    protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
        return named("handler")
                .and(takesArguments(Handler.class))
                .and(returns(Route.class));
    }

    @Advice.OnMethodEnter
    public static void monitorStart(@Advice.Argument(value = 0, readOnly = false) Handler<RoutingContext> handler) {
        handler = new RequestMonitoringHandler(handler);
    }
}
