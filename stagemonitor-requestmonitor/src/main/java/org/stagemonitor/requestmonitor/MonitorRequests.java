package org.stagemonitor.requestmonitor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * By annotating a type with {@link MonitorRequests}, a Timer will be created for all its public methods and the call stack
 * of these methods will be recorded. It is also possible to annotate at method level.
 * <p/>
 * This annotation is inherited, that means that any subtype of an annotated class or method will also be monitored.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface MonitorRequests {

	/**
	 * Customizes the {@link RequestTrace#name}.
	 * <p/>
	 * If not explicitly, the request name is set according to the {@link RequestMonitorPlugin#businessTransactionNamingStrategy}.
	 *
	 * This is only applicable on the method level.
	 *
	 * @return the request name
	 */
	String requestName() default "";

	/**
	 * If set to true, the {@link RequestTrace#name} will be determined at runtime according to the
	 * according to the {@link RequestMonitorPlugin#businessTransactionNamingStrategy}.
	 * <p/>
	 * This is a bit slower (nothing to worry about though) but the request is evaluated at runtime which means it
	 * could contain the name of a subclass.
	 *
	 * @return <code>true</code> if the request name should be evaluated at runtime, <code>false</code> otherwise.
	 */
	boolean resolveNameAtRuntime() default false;
}
