package org.stagemonitor.vertx.transformers;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.vertx.VertxPlugin;
import org.stagemonitor.vertx.wrappers.MessageConsumerMonitoringHandler;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class EventBusConsumerTransformer extends StagemonitorByteBuddyTransformer {

    public static Logger logger = LoggerFactory.getLogger(EventBusConsumerTransformer.class);

    private static VertxPlugin vertxPlugin = Stagemonitor.getPlugin(VertxPlugin.class);

	@Override
    protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
        return EventBusConsumerTransformer.class;
    }

    @Override
    protected ElementMatcher.Junction<TypeDescription> getNarrowTypesMatcher() {
        return named(vertxPlugin.getEventBusImplementation());
    }

    @Override
    protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return takesArguments(String.class, Handler.class)
				.and(returns(MessageConsumer.class));
    }

    @Advice.OnMethodEnter
    public static void wrap(@Advice.Argument(value = 1, readOnly = false) Handler<Message<?>> handler) throws NoSuchFieldException, IllegalAccessException {
		handler = new MessageConsumerMonitoringHandler(handler);
    }
}
