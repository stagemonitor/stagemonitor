package org.stagemonitor.vertx.transformers;


import io.vertx.rxjava.core.eventbus.Message;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import rx.Observable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class ObservableMonitorRequestTransformer extends StagemonitorByteBuddyTransformer{

    @Override
    protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
        return ObservableMonitorRequestTransformer.class;
    }

    @Override
    protected ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
        return named("rx.Observable");
    }

    @Override
    protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getMethodElementMatcher() {
        return isPublic()
                .and(returns(Observable.class));
    }

    @Advice.OnMethodEnter
    public static String checkArgs(@Advice.AllArguments Object[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String behavior = "DEFAULT";
//        Class<?> observableWrapper = Class.forName("org.stagemonitor.vertx.wrappers.ObservableWrapper");
//        for (Object arg : args) {
//            if (observableWrapper.isInstance(arg)) {
//                behavior = (String) observableWrapper.getDeclaredMethod("getBehavior").invoke(arg);
//                break;
//            } else if (arg instanceof Iterable<?>) {
//                for (Object obj : (Iterable<?>) arg) {
//                    if (observableWrapper.isInstance(obj)) {
//                        behavior = (String) observableWrapper.getDeclaredMethod("getBehavior").invoke(arg);
//                        break;
//                    }
//                }
//            }
//        }
        return behavior;
    }

    @Advice.OnMethodExit
    public static void wrapObservable(@Advice.Return(readOnly = false) Observable<Message<?>> observable, @Advice.Enter String behavior) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
//        if (!behavior.equals("DEFAULT")) {
//            Constructor constructor = Class.forName("org.stagemonitor.vertx.wrappers.ObservableWrapper").getConstructor(Observable.class, String.class);
//            observable = (Observable<Message<?>>) constructor.newInstance(observable, behavior);
//        }
    }
}
