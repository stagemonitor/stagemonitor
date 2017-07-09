package org.stagemonitor.tracing;

import com.codahale.metrics.Timer;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.scheduling.annotation.Async;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.Metric2Filter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.tracing.utils.SpanUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class TracedTransformerTest {

	private TestClass testClass;
	private TestClassLevelAnnotationClass testClassLevelAnnotationClass;
	private Metric2Registry metricRegistry;
	private SpanCapturingReporter spanCapturingReporter;
	private Map<String, Object> tags;

	@BeforeClass
	public static void attachProfiler() {
		Stagemonitor.init();
	}

	@Before
	public void before() throws Exception {
		testClass = new TestSubClass();
		metricRegistry = Stagemonitor.getMetric2Registry();
		testClassLevelAnnotationClass = new TestClassLevelAnnotationClass();
		metricRegistry.removeMatching(Metric2Filter.ALL);
		Stagemonitor.setMeasurementSession(new MeasurementSession("TracedTransformerTest", "test", "test"));
		Stagemonitor.startMonitoring();
		spanCapturingReporter = new SpanCapturingReporter();
		tags = new HashMap<>();
		Stagemonitor.getPlugin(TracingPlugin.class).addSpanEventListenerFactory(new TagRecordingSpanEventListener(tags));
		Stagemonitor.getPlugin(TracingPlugin.class).addReporter(spanCapturingReporter);
	}

	@AfterClass
	public static void resetStagemonitor() {
		Stagemonitor.reset();
	}

	@Test
	public void testMonitorRequests() throws Exception {
		testClass.monitorMe(1);
		final SpanContextInformation spanContext = spanCapturingReporter.get();
		// either parameters.arg0 or parameters.s
		assertThat("1").isEqualTo(getTagsStartingWith(tags, SpanUtils.PARAMETERS_PREFIX).iterator().next());
		assertThat("TracedTransformerTest$TestClass#monitorMe").isEqualTo(spanContext.getOperationName());
		assertThat(1).isEqualTo(spanContext.getCallTree().getChildren().size());
		final String signature = spanContext.getCallTree().getChildren().get(0).getSignature();
		assertThat(signature).contains("org.stagemonitor.tracing.TracedTransformerTest$TestClass.monitorMe");

		final Map<MetricName,Timer> timers = metricRegistry.getTimers();
		assertThat(timers).containsKey(name("response_time").operationName("TracedTransformerTest$TestClass#monitorMe").operationType("method_invocation").build());
	}

	@Test
	public void testMonitorAsyncMethods() throws Exception {
		testClass.asyncMethod();
		final SpanContextInformation spanContext = spanCapturingReporter.get();
		assertThat("TracedTransformerTest$TestClass#asyncMethod").isEqualTo(spanContext.getOperationName());
		assertThat(1).isEqualTo(spanContext.getCallTree().getChildren().size());
		final String signature = spanContext.getCallTree().getChildren().get(0).getSignature();
		assertThat(signature).contains("org.stagemonitor.tracing.TracedTransformerTest$TestClass.asyncMethod");

		final Map<MetricName,Timer> timers = metricRegistry.getTimers();
		assertThat(timers).containsKey(name("response_time").operationName("TracedTransformerTest$TestClass#asyncMethod").operationType("method_invocation").build());
	}

	@Test
	public void testDontMonitorAnySuperMethod() throws Exception {
		testClass.dontMonitorMe();
		assertThat(spanCapturingReporter.get()).isNull();
	}

	@Test
	public void testMonitorRequestsThrowingException() throws Exception {
		assertThatThrownBy(() -> testClass.monitorThrowException()).isInstanceOf(NullPointerException.class);
		assertThat(NullPointerException.class.getName()).isEqualTo(tags.get("exception.class"));

		final Map<MetricName,Timer> timers = metricRegistry.getTimers();
		assertThat(timers).containsKey(name("response_time").operationName("TracedTransformerTest$TestClass#monitorThrowException").operationType("method_invocation").build());
	}

	@Test
	public void testMonitorRequestsAnnonymousInnerClass() throws Exception {
		testClass.monitorAnnonymousInnerClass();
		final SpanContextInformation spanContext = spanCapturingReporter.get();
		assertThat("TracedTransformerTest$TestClass$1#run").isEqualTo(spanContext.getOperationName());
		assertThat(1).isEqualTo(spanContext.getCallTree().getChildren().size());
		final String signature = spanContext.getCallTree().getChildren().get(0).getSignature();
		assertThat(signature).contains("org.stagemonitor.tracing.TracedTransformerTest$TestClass$1.run");

		final Map<MetricName,Timer> timers = metricRegistry.getTimers();
		assertThat(timers).containsKey(name("response_time").operationName("TracedTransformerTest$TestClass$1#run").operationType("method_invocation").build());
	}

	@Test
	public void testMonitorRequestsResolvedAtRuntime() throws Exception {
		testClass.resolveNameAtRuntime();
		final String operationName = spanCapturingReporter.get().getOperationName();
		assertThat("TracedTransformerTest$TestSubClass#resolveNameAtRuntime").isEqualTo(operationName);
	}

	@Test
	public void testMonitorStaticMethod() throws Exception {
		TestClass.monitorStaticMethod();
		final String operationName = spanCapturingReporter.get().getOperationName();
		assertThat("TracedTransformerTest$TestClass#monitorStaticMethod").isEqualTo(operationName);
	}

	@Test
	public void testMonitorRequestsCustomName() throws Exception {
		testClass.doFancyStuff();
		final String operationName = spanCapturingReporter.get().getOperationName();
		assertThat("My Cool Method").isEqualTo(operationName);
	}

	private static abstract class SuperAbstractTestClass {
		@Traced
		public abstract int monitorMe(int i) throws Exception;
	}

	private static abstract class AbstractTestClass extends SuperAbstractTestClass{
		public abstract void dontMonitorMe() throws Exception;
	}

	private static class TestClass extends AbstractTestClass {
		@Traced
		public int monitorMe(int i) throws Exception {
			return i;
		}

		@Override
		public void dontMonitorMe() throws Exception {
		}

		@Traced(resolveNameAtRuntime = true)
		public void resolveNameAtRuntime() throws Exception {
		}

		@Traced
		public static void monitorStaticMethod() {
		}

		@Traced(requestName = "My Cool Method")
		public void doFancyStuff() throws Exception {
		}

		public void monitorAnnonymousInnerClass() {
			new Runnable() {
				@Override
				@Traced
				public void run() {

				}
			}.run();
		}

		@Traced
		public int monitorThrowException() throws Exception {
			throw null;
		}

		@Async
		public void asyncMethod() {
		}
	}

	private static class TestSubClass extends TestClass {
	}

	@Test
	public void testClassLevelAnnotationClass() throws Exception {
		testClassLevelAnnotationClass.monitorMe("1");
		testClassLevelAnnotationClass.dontMonitorMe();
		final SpanContextInformation spanContext = spanCapturingReporter.get();

		// either parameters.arg0 or parameters.s
		assertThat("1").isEqualTo(getTagsStartingWith(tags, SpanUtils.PARAMETERS_PREFIX).iterator().next());
		assertThat("TracedTransformerTest$TestClassLevelAnnotationClass#monitorMe").isEqualTo(spanContext.getOperationName());
		assertThat(1).isEqualTo(spanContext.getCallTree().getChildren().size());
		final String signature = spanContext.getCallTree().getChildren().get(0).getSignature();
		assertThat(signature).contains("org.stagemonitor.tracing.TracedTransformerTest$TestClassLevelAnnotationClass.monitorMe");

		final Map<MetricName, Timer> timers = metricRegistry.getTimers();
		assertThat(timers).containsKey(name("response_time").operationName("TracedTransformerTest$TestClassLevelAnnotationClass#monitorMe").operationType("method_invocation").build());
	}

	@Traced
	private static class SuperTestClassLevelAnnotationClass {
	}

	private static class TestClassLevelAnnotationClass extends SuperTestClassLevelAnnotationClass {

		public String monitorMe(String s) throws Exception {
			return s;
		}

		private int dontMonitorMe() throws Exception {
			return 0;
		}

	}

	private List<Object> getTagsStartingWith(Map<String, Object> tags, String prefix) {
		List<Object> tagValuesStartingWith = new ArrayList<Object>();
		for (Map.Entry<String, Object> entry : tags.entrySet()) {
			if (entry.getKey().startsWith(prefix)) {
				tagValuesStartingWith.add(entry.getValue());
			}
		}
		return tagValuesStartingWith;
	}

}
