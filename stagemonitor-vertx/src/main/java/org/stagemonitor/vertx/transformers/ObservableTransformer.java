package org.stagemonitor.vertx.transformers;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import rx.Subscriber;
import rx.Subscription;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class ObservableTransformer extends StagemonitorByteBuddyTransformer {

    @Override
    protected ElementMatcher.Junction<TypeDescription> getNarrowTypesMatcher() {
        return named("rx.Observable");
    }

    @Override
    protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getMethodElementMatcher() {
        return not(isConstructor())
                .and(not(isAbstract()))
                .and(not(isNative()))
                .and(not(isSynthetic()))
                .and(not(isTypeInitializer()))
                .and(named("subscribe"))
                .and(takesArguments(Subscriber.class))
                .and(returns(Subscription.class));
    }

    private void bob(AgentBuilder builder){
	}

    @Override
    public AgentBuilder.Transformer getTransformer() {
        return new AgentBuilder.Transformer() {

            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
                //This is where I need to change the modifiers of a specific method
                return builder.transform();
            }
        };
    }
}
