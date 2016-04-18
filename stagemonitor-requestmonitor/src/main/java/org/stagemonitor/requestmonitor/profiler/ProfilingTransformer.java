package org.stagemonitor.requestmonitor.profiler;

import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.core.instrument.StagemonitorClassNameMatcher;

public class ProfilingTransformer extends StagemonitorByteBuddyTransformer {

	@Override
	public ElementMatcher.Junction<TypeDescription> getExtraExcludeTypeMatcher() {
		return nameStartsWith(Profiler.class.getPackage().getName())
				.or(makeSureClassesAreNotProfiledTwice());
	}

	/*
	 * If this is a subclass of ProfilingTransformer, make sure to not instrument classes
	 * which are matched by ProfilingTransformer
	 */
	private ElementMatcher.Junction<TypeDescription> makeSureClassesAreNotProfiledTwice() {
		return isSubclass() ? StagemonitorClassNameMatcher.INSTANCE : ElementMatchers.<TypeDescription>none();
	}

	private boolean isSubclass() {
		return getClass() != ProfilingTransformer.class;
	}

	@Override
	protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
		return ProfilingTransformer.class;
	}

	@Advice.OnMethodEnter
	public static void enter(@Advice.Origin("#r #t.#m#s") String signature) {
		Profiler.start(signature);
	}

	@Advice.OnMethodExit
	public static void exit() {
		Profiler.stop();
	}

	@Override
	protected int getOrder() {
		return Integer.MAX_VALUE;
	}
}
