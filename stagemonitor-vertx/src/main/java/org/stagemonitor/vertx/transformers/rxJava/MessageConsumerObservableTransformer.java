package org.stagemonitor.vertx.transformers.rxJava;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.rxjava.core.eventbus.Message;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.vertx.VertxPlugin;
import org.stagemonitor.vertx.wrappers.rxJava.ObservableWrapper;
import rx.Observable;

import java.lang.reflect.Field;

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
    public static void wrap(@Advice.Return(readOnly = false) Observable<Message<?>> observable) throws NoSuchFieldException, IllegalAccessException {
		Field onSubscribe = observable.getClass().getDeclaredField("onSubscribe");
        onSubscribe.setAccessible(true);
		observable = new ObservableWrapper((Observable.OnSubscribe) onSubscribe.get(observable), "MONITORING_MESSAGE");
    }
}
