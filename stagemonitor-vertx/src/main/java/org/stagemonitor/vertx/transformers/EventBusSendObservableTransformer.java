package org.stagemonitor.vertx.transformers;

import com.uber.jaeger.context.TracingUtils;
import io.vertx.rxjava.core.eventbus.Message;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.vertx.RequestKeeper;
import rx.Observable;

import java.lang.reflect.InvocationTargetException;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class EventBusSendObservableTransformer extends StagemonitorByteBuddyTransformer {

    public static Logger logger = LoggerFactory.getLogger(EventBusSendObservableTransformer.class);

    @Override
    protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
        return EventBusSendObservableTransformer.class;
    }

    @Override
    protected ElementMatcher.Junction<TypeDescription> getNarrowTypesMatcher() {
        return named("io.vertx.rxjava.core.eventbus.EventBus");
    }

    @Override
    protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
        return named("sendObservable")
                .and(returns(Observable.class))
                .and(takesArguments(2));
    }

    @Advice.OnMethodEnter
    public static void storeContext(@Advice.Argument(value = 1) Object message){
		RequestKeeper.getInstance().storeContext(message, TracingUtils.getTraceContext());
    }

    @Advice.OnMethodExit
    @SuppressWarnings("unchecked")
    public static void wrapObservable(@Advice.Return(readOnly = false) Observable<Message<?>> observable, @Advice.This Object messageConsumer ) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
//        Constructor constructor = Class.forName("org.stagemonitor.vertx.wrappers.ObservableWrapper").getConstructor(Observable.class, String.class);
//        observable = (Observable<Message<?>>) constructor.newInstance(observable, "MONITORING_RESPONSE");
    }
}
