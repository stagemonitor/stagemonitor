package org.stagemonitor.requestmonitor.ejb;

import com.uber.jaeger.context.TracingUtils;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.SpanCapturingReporter;
import org.stagemonitor.requestmonitor.SpanContextInformation;

import javax.ejb.Remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RemoteEjbMonitorTransformerTest {

	private RemoteInterface remote = new RemoteInterfaceImpl();
	private RemoteInterfaceWithRemoteAnnotation remoteAlt = new RemoteInterfaceWithRemoteAnnotationImpl();
	private SpanCapturingReporter spanCapturingReporter;

	@BeforeClass
	@AfterClass
	public static void reset() {
		Stagemonitor.reset();
	}

	@Before
	public void setUp() throws Exception {
		assertTrue(TracingUtils.getTraceContext().isEmpty());
		spanCapturingReporter = new SpanCapturingReporter();
		Stagemonitor.getPlugin(RequestMonitorPlugin.class).addReporter(spanCapturingReporter);
	}

	@After
	public void tearDown() throws Exception {
		assertTrue(TracingUtils.getTraceContext().isEmpty());
	}

	@Test
	public void testMonitorRemoteCalls() throws Exception {
		remote.foo();

		final SpanContextInformation spanContext = spanCapturingReporter.get();
		assertNotNull(spanContext.getSpan());
		assertEquals("RemoteEjbMonitorTransformerTest$RemoteInterfaceImpl#foo", spanContext.getOperationName());
		assertFalse(spanContext.getCallTree().toString(), spanContext.getCallTree().getChildren().isEmpty());
		final String signature = spanContext.getCallTree().getChildren().get(0).getSignature();
		assertTrue(signature, signature.contains("org.stagemonitor.requestmonitor.ejb.RemoteEjbMonitorTransformerTest$RemoteInterfaceImpl"));
	}

	@Test
	public void testMonitorRemoteCallsAlternateHierarchy() throws Exception {
		remoteAlt.bar();

		final SpanContextInformation spanContext = spanCapturingReporter.get();
		assertNotNull(spanContext.getSpan());
		assertEquals("RemoteEjbMonitorTransformerTest$RemoteInterfaceWithRemoteAnnotationImpl#bar", spanContext.getOperationName());
		assertFalse(spanContext.getCallTree().toString(), spanContext.getCallTree().getChildren().isEmpty());
		final String signature = spanContext.getCallTree().getChildren().get(0).getSignature();
		assertTrue(signature, signature.contains("org.stagemonitor.requestmonitor.ejb.RemoteEjbMonitorTransformerTest$RemoteInterfaceWithRemoteAnnotationImpl"));
	}

	@Test
	public void testMonitorRemoteCallsSuperInterface() throws Exception {
		remoteAlt.foo();

		final SpanContextInformation spanContext = spanCapturingReporter.get();
		assertNotNull(spanContext.getSpan());
		assertEquals("RemoteEjbMonitorTransformerTest$RemoteInterfaceWithRemoteAnnotationImpl#foo", spanContext.getOperationName());
		assertFalse(spanContext.getCallTree().toString(), spanContext.getCallTree().getChildren().isEmpty());
		final String signature = spanContext.getCallTree().getChildren().get(0).getSignature();
		assertTrue(signature, signature.contains("org.stagemonitor.requestmonitor.ejb.RemoteEjbMonitorTransformerTest$RemoteInterfaceWithRemoteAnnotationImpl"));
	}


	@Test
	public void testExcludeGeneratedClasses() throws Exception {
		// classes which contain $$ are usually generated classes
		new $$ExcludeGeneratedClasses().bar();
		assertNull(spanCapturingReporter.get());
	}

	@Test
	public void testDontMonitorToString() throws Exception {
		remote.toString();

		assertNull(spanCapturingReporter.get());
	}

	@Test
	public void testDontMonitorNonRemoteEjb() throws Exception {
		new NoRemoteEJB().foo();

		assertNull(spanCapturingReporter.get());
	}

	private interface SuperInterface {
		void foo();
	}

	interface RemoteInterface extends SuperInterface {
	}

	@Remote(RemoteInterface.class)
	public class RemoteInterfaceImpl implements RemoteInterface {

		@Override
		public void foo() {
		}

		@Override
		public String toString() {
			return super.toString();
		}
	}

	@Remote
	private interface RemoteInterfaceWithRemoteAnnotation extends RemoteInterface {
		void bar();
	}

	public class RemoteInterfaceWithRemoteAnnotationImpl implements RemoteInterfaceWithRemoteAnnotation {

		@Override
		public void bar() {
		}

		@Override
		public String toString() {
			return super.toString();
		}

		@Override
		public void foo() {
		}
	}

	public class NoRemoteEJB implements SuperInterface {
		@Override
		public void foo() {
		}
	}

	public class $$ExcludeGeneratedClasses implements RemoteInterfaceWithRemoteAnnotation {

		@Override
		public void bar() {
		}

		@Override
		public void foo() {
		}
	}

}
