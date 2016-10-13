package org.stagemonitor.requestmonitor.ejb;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.requestmonitor.SpanCapturingReporter;

import javax.ejb.Remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RemoteEjbMonitorTransformerTest {

	private RemoteInterface remote = new RemoteInterfaceImpl();
	private RemoteInterfaceWithRemoteAnnotation remoteAlt = new RemoteInterfaceWithRemoteAnnotationImpl();
	private SpanCapturingReporter requestTraceCapturingReporter = new SpanCapturingReporter();

	@BeforeClass
	@AfterClass
	public static void reset() {
		Stagemonitor.reset();
	}

	@Test
	public void testMonitorRemoteCalls() throws Exception {
		remote.foo();

		RequestTrace requestTrace = requestTraceCapturingReporter.get();
		assertNotNull(requestTrace);
		assertEquals("RemoteEjbMonitorTransformerTest$RemoteInterfaceImpl#foo", requestTrace.getName());
		assertFalse(requestTrace.getCallStack().toString(), requestTrace.getCallStack().getChildren().isEmpty());
		final String signature = requestTrace.getCallStack().getChildren().get(0).getSignature();
		assertTrue(signature, signature.contains("org.stagemonitor.requestmonitor.ejb.RemoteEjbMonitorTransformerTest$RemoteInterfaceImpl"));
	}

	@Test
	public void testMonitorRemoteCallsAlternateHierarchy() throws Exception {
		remoteAlt.bar();

		RequestTrace requestTrace = requestTraceCapturingReporter.get();
		assertNotNull(requestTrace);
		assertEquals("RemoteEjbMonitorTransformerTest$RemoteInterfaceWithRemoteAnnotationImpl#bar", requestTrace.getName());
		assertFalse(requestTrace.getCallStack().toString(), requestTrace.getCallStack().getChildren().isEmpty());
		final String signature = requestTrace.getCallStack().getChildren().get(0).getSignature();
		assertTrue(signature, signature.contains("org.stagemonitor.requestmonitor.ejb.RemoteEjbMonitorTransformerTest$RemoteInterfaceWithRemoteAnnotationImpl"));
	}

	@Test
	public void testMonitorRemoteCallsSuperInterface() throws Exception {
		remoteAlt.foo();

		RequestTrace requestTrace = requestTraceCapturingReporter.get();
		assertNotNull(requestTrace);
		assertEquals("RemoteEjbMonitorTransformerTest$RemoteInterfaceWithRemoteAnnotationImpl#foo", requestTrace.getName());
		assertFalse(requestTrace.getCallStack().toString(), requestTrace.getCallStack().getChildren().isEmpty());
		final String signature = requestTrace.getCallStack().getChildren().get(0).getSignature();
		assertTrue(signature, signature.contains("org.stagemonitor.requestmonitor.ejb.RemoteEjbMonitorTransformerTest$RemoteInterfaceWithRemoteAnnotationImpl"));
	}


	@Test
	public void testExcludeGeneratedClasses() throws Exception {
		// classes which contain $$ are usually generated classes
		new $$ExcludeGeneratedClasses().bar();
		assertNull(requestTraceCapturingReporter.get());
	}

	@Test
	public void testDontMonitorToString() throws Exception {
		remote.toString();

		RequestTrace requestTrace = requestTraceCapturingReporter.get();
		assertNull(requestTrace);
	}

	@Test
	public void testDontMonitorNonRemoteEjb() throws Exception {
		new NoRemoteEJB().foo();

		RequestTrace requestTrace = requestTraceCapturingReporter.get();
		assertNull(requestTrace);
	}

	private interface SuperInterface {
		void foo();
	}

	private interface RemoteInterface extends SuperInterface {
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
