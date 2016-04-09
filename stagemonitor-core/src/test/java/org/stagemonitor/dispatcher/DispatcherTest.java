package org.stagemonitor.dispatcher;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.Dispatcher;

public class DispatcherTest {

	@Before
	public void setUp() throws Exception {
		Stagemonitor.init();
	}

	@Test
	public void testInjectDispatcherToBootstrapClasspath() throws ClassNotFoundException {
		Dispatcher.put("foo", "bar");
		Assert.assertEquals("bar", Dispatcher.get("foo"));
	}

}