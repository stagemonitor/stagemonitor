package org.stagemonitor.vertx.transformers;

import com.uber.jaeger.context.TracingUtils;
import io.vertx.core.eventbus.EventBus;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.vertx.RequestKeeper;
import org.stagemonitor.vertx.VertxPlugin;
import rx.Observable;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class EventBusSendTransformer extends StagemonitorByteBuddyTransformer {

    public static Logger logger = LoggerFactory.getLogger(EventBusSendTransformer.class);
	private static VertxPlugin vertxPlugin = Stagemonitor.getPlugin(VertxPlugin.class);

    @Override
    protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
        return EventBusSendTransformer.class;
    }

    @Override
    protected ElementMatcher.Junction<TypeDescription> getNarrowTypesMatcher() {
        return named(vertxPlugin.getEventBusImplementation());
    }

    @Override
    protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
        return (named("send")
				.or(named("publish")))
                .and(returns(EventBus.class));
    }

    @Advice.OnMethodEnter
    public static void storeContext(@Advice.Argument(value = 1) Object message){
    	if(!RequestKeeper.getInstance().containsKey(message)){
			RequestKeeper.getInstance().storeContext(message, TracingUtils.getTraceContext());
		}
    }
}
