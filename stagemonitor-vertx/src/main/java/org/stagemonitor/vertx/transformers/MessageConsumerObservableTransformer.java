package org.stagemonitor.vertx.transformers;

import io.vertx.rxjava.core.eventbus.Message;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import rx.Observable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class MessageConsumerObservableTransformer extends StagemonitorByteBuddyTransformer {

    public static Logger logger = LoggerFactory.getLogger(MessageConsumerObservableTransformer.class);

    @Override
    protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
        return MessageConsumerObservableTransformer.class;
    }

    @Override
    protected ElementMatcher.Junction<TypeDescription> getNarrowTypesMatcher() {
        return named("io.vertx.rxjava.core.eventbus.MessageConsumer");
    }

    @Override
    protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
        return named("toObservable")
                .and(returns(Observable.class))
                .and(takesArguments(0));
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    @SuppressWarnings("unchecked")
    public static void wrapObservable(@Advice.Return(readOnly = false) Observable<Message<?>> observable) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
//		Constructor constructor = Class.forName("org.stagemonitor.vertx.wrappers.ObservableWrapper").getConstructor(Observable.class, String.class);
//        observable = (Observable<Message<?>>) constructor.newInstance(observable, "MONITORING_MESSAGE");
    }
}
