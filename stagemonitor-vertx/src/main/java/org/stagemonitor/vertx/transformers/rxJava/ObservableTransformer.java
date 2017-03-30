package org.stagemonitor.vertx.transformers.rxJava;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.vertx.wrappers.rxJava.MessageConsumerMonitoringSubscriber;
import org.stagemonitor.vertx.wrappers.rxJava.ObservableWrapper;

import io.vertx.rxjava.core.eventbus.Message;
import rx.Subscriber;
import rx.Subscription;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public class ObservableTransformer extends StagemonitorByteBuddyTransformer {

	@Advice.OnMethodEnter
	public static void intercept(@Advice.Argument(value = 0, readOnly = false) Subscriber<Message<?>> sub, @Advice.This Object instance) {
		if (instance instanceof ObservableWrapper) {
			ObservableWrapper observableWrapper = (ObservableWrapper) instance;
			switch (observableWrapper.getBehavior()) {
				case "MONITORING_MESSAGE":
					sub = new MessageConsumerMonitoringSubscriber(sub);
					break;
			}
		}
	}

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
}
