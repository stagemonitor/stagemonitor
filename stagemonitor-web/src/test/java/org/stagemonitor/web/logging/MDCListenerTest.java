package org.stagemonitor.web.logging;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;

import com.codahale.metrics.SharedMetricRegistries;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;

public class MDCListenerTest {

	private MDCListener mdcListener;
	private CorePlugin corePlugin;

	@Before
	public void setUp() throws Exception {
		Stagemonitor.reset();
		SharedMetricRegistries.clear();
		this.corePlugin = mock(CorePlugin.class);
		when(corePlugin.isStagemonitorActive()).thenReturn(true);

		mdcListener = new MDCListener(corePlugin);
	}

	@After
	public void tearDown() throws Exception {
		Stagemonitor.reset();
		MDC.clear();
	}

	@Test
	public void testMDCInstanceAlreadySet() throws Exception {
		Stagemonitor.startMonitoring(new MeasurementSession("MDCListenerTest", "testHost", "testInstance")).get();
		when(corePlugin.getMeasurementSession()).thenReturn(new MeasurementSession("MDCListenerTest", "testHost", "testInstance"));
		final ServletRequestEvent requestEvent = mock(ServletRequestEvent.class);
		final ServletRequest request = mock(ServletRequest.class);
		when(requestEvent.getServletRequest()).thenReturn(request);
		mdcListener.requestInitialized(requestEvent);

		Assert.assertNotNull(MDC.get("requestId"));
		verify(request).setAttribute(eq(MDCListener.STAGEMONITOR_REQUEST_ID_ATTR), anyString());

		mdcListener.requestDestroyed(requestEvent);
		Assert.assertEquals("testHost", MDC.get("host"));
		Assert.assertEquals("MDCListenerTest", MDC.get("application"));
		Assert.assertEquals("testInstance", MDC.get("instance"));
		Assert.assertNull(MDC.get("requestId"));
	}

	@Test
	public void testMDCInstanceNotAlreadySet() throws Exception {
		when(corePlugin.getMeasurementSession()).thenReturn(new MeasurementSession("MDCListenerTest", "testHost", null));

		final ServletRequestEvent requestEvent = mock(ServletRequestEvent.class);
		final ServletRequest request = mock(ServletRequest.class);
		when(request.getServerName()).thenReturn("testInstance");
		when(requestEvent.getServletRequest()).thenReturn(request);

		mdcListener.requestInitialized(requestEvent);
		Assert.assertEquals("testHost", MDC.get("host"));
		Assert.assertEquals("MDCListenerTest", MDC.get("application"));
		Assert.assertEquals("testInstance", MDC.get("instance"));
		Assert.assertNull(MDC.get("requestId"));
		mdcListener.requestDestroyed(requestEvent);
	}

	@Test
	public void testMDCStagemonitorDeactivated() throws Exception {
		when(corePlugin.isStagemonitorActive()).thenReturn(false);
		when(corePlugin.getMeasurementSession()).thenReturn(new MeasurementSession("MDCListenerTest", "testHost", null));

		mdcListener.requestInitialized(mock(ServletRequestEvent.class));

		Assert.assertNull(MDC.getCopyOfContextMap());
	}
}
