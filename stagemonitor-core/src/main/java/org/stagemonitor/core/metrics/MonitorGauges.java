package org.stagemonitor.core.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When a type is marked with this annotation, the creation of gauges with
 * @{@link com.codahale.metrics.annotation.Gauge} is activated for that type.
 *
 * <pre><code>
 *     \@MonitorGauges
 *     public class Queue {
 *         \@Gauge(name = "queueSize")
 *         public int getQueueSize() {
 *             return queue.size;
 *         }
 *     }
 * </code></pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MonitorGauges {
}
