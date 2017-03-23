package org.stagemonitor.vertx.transformers.rxJava;

import io.vertx.rxjava.core.eventbus.Message;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.Super;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.vertx.wrappers.rxJava.MessageConsumerMonitoringSubscriber;
import org.stagemonitor.vertx.wrappers.rxJava.ObservableWrapper;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class ObservableTransformer extends StagemonitorByteBuddyTransformer {

	@Override
	protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
		return ObservableTransformer.class;
	}

	@Override
    protected ElementMatcher.Junction<TypeDescription> getNarrowTypesMatcher() {
        return named("rx.Observable");
    }

	@Override
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getMethodElementMatcher() {
		return named("subscribe")
			.and(takesArguments(Subscriber.class))
			.and(returns(Subscription.class));
	}

	@Advice.OnMethodEnter
	public static void intercept(@Advice.Argument(value = 0, readOnly = false) Subscriber<Message<?>> sub, @Advice.This Object instance) {
		if(instance instanceof ObservableWrapper){
			ObservableWrapper observableWrapper = (ObservableWrapper) instance;
			switch (observableWrapper.getBehavior()){
				case "MONITORING_MESSAGE":
					sub = new MessageConsumerMonitoringSubscriber(sub);
					break;
			}
		}
	}
}
