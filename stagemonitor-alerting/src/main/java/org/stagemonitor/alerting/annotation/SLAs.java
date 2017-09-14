package org.stagemonitor.alerting.annotation;

import org.stagemonitor.tracing.Traced;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be used to create multiple Checks for a method.
 * <p>
 * The method also has to be annotated with @{@link Traced}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SLAs {
	SLA[] value();
}
