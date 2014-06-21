package org.stagemonitor.collector.monitor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * By annotating a type with {@link Monitored}, a Timer will be created for all its public methods and the call stack
 * of these methods will be recorded. It is also possible to annotate at method level.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Monitored {
}
