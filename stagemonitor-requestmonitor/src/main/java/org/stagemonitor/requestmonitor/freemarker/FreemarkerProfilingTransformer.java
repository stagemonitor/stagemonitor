package org.stagemonitor.requestmonitor.freemarker;

import freemarker.core.Environment;
import freemarker.core.Expression;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * This class "profiles FTL files"
 * <p/>
 * This class actually adds expressions and methods which are evaluated by freemarker to the call tree. So that when a
 * model class of our application is called, we know which FTL file originated the call.
 * <p/>
 * You may argue that this is not of particular interest because model objects are mostly POJOs and calling a getter
 * is not interesting performance wise. This is true for POJOs but some applications may choose to not fully initialize
 * the model objects but instead lazy-load the values on demand i.e. if they are actually needed for the template.
 * <p/>
 * Example:
 *
 * <pre>
 * </code>
 * test.ftl:1#templateModel.allTheThings
 * `-- String org.example.TemplateModel.getAllTheThings()
 *     `-- String org.example.ExampleDao.getAllTheThingsFromDB()
 *         `-- SELECT * from THINGS
 * </code>
 * </pre>
 */
public class FreemarkerProfilingTransformer extends StagemonitorByteBuddyTransformer {

	/**
	 * Application code can be called by freemarker via the {@link freemarker.core.Dot} or the
	 * {@link freemarker.core.MethodCall} classes.
	 * <p/>
	 * For example, when the expression ${templateModel.foo} is evaluated, {@link freemarker.core.Dot#_eval(Environment)}
	 * evaluates <code>foo</code> by calling <code>TemplateModel#getFoo()</code>.
	 * <p/>
	 * The expression ${templateModel.getFoo()} will be evaluated a bit differently as {@link freemarker.core.Dot#_eval(Environment)}
	 * only returns a reference to the method <code>TemplateModel#getFoo()</code> instead of calling it directly.
	 * {@link freemarker.core.MethodCall#_eval(Environment)} then performs the actual call to <code>TemplateModel#getFoo()</code>.
	 */
	@Override
	protected ElementMatcher.Junction<TypeDescription> getIncludeTypeMatcher() {
		return named("freemarker.core.Dot").or(named("freemarker.core.MethodCall"));
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return named("_eval").and(takesArguments(Environment.class));
	}

	@Advice.OnMethodEnter(inline = false)
	public static void onBeforeEvaluate(@Advice.Argument(0) Environment env, @Advice.This Expression dot) {
		Profiler.start(env.getCurrentTemplate().getName() + ':' + dot.getBeginLine() + '#' + dot.toString());
	}

	@Advice.OnMethodExit(inline = false, onThrowable = Throwable.class)
	public static void onAfterEvaluate() {
		final CallStackElement currentFreemarkerCall = Profiler.getMethodCallParent();
		Profiler.stop();
		removeCurrentNodeIfItHasNoChildren(currentFreemarkerCall);
	}

	/**
	 * <pre>
	 * </code>
	 * test.ftl:1#templateModel.getFoo() <- added by {@link freemarker.core.MethodCall#_eval(Environment)}
	 * |-- test.ftl:1#templateModel.getFoo <- added by {@link freemarker.core.Dot#_eval(Environment)}
	 * `-- String org.stagemonitor.requestmonitor.freemarker.FreemarkerProfilingTest$TemplateModel.getFoo()
	 * </code>
	 * </pre>
	 *
	 * We want to remove <code>templateModel.getFoo</code> as getFoo only returns the method reference, which is then
	 * invoked by {@link freemarker.core.MethodCall#_eval(Environment)}.
	 * Therefore, <code>getFoo</code> does not invoke the model and thus is not relevant for the call tree
	 */
	private static void removeCurrentNodeIfItHasNoChildren(CallStackElement currentFreemarkerCall) {
		if (currentFreemarkerCall.getChildren().isEmpty()) {
			currentFreemarkerCall.remove();
		}
	}
}
