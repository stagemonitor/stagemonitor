package org.stagemonitor.jdbc;

import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.jdbc.ReflectiveConnectionMonitoringTransformer.ConnectionMonitorAddingSpanEventListener;
import org.stagemonitor.tracing.wrapper.SpanEventListenerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

public class TestServiceLoader {

	@Test
	public void testLoadConnectionMonitorAddingSpanEventListener() throws Exception {
		Stagemonitor.init();

		assertThat(getFactoryClasses()).contains(ConnectionMonitorAddingSpanEventListener.class);
	}

	private List<Class<? extends SpanEventListenerFactory>> getFactoryClasses() {
		List<Class<? extends SpanEventListenerFactory>> spanEventListenerFactories = new ArrayList<>();
		final ServiceLoader<SpanEventListenerFactory> serviceLoader = ServiceLoader.load(SpanEventListenerFactory.class);
		serviceLoader.forEach(e -> spanEventListenerFactories.add(e.getClass()));
		return spanEventListenerFactories;
	}
}
