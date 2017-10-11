package org.stagemonitor.tracing;

import java.lang.reflect.Field;

import io.opentracing.NoopTracerFactory;
import io.opentracing.util.GlobalTracer;

public class GlobalTracerTestHelper {

	public static void resetGlobalTracer() {
		try {
			final Field tracerField = GlobalTracer.class.getDeclaredField("tracer");
			tracerField.setAccessible(true);
			tracerField.set(null, NoopTracerFactory.create());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
