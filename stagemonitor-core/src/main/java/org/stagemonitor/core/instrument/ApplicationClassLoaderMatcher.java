package org.stagemonitor.core.instrument;

import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.ClassUtils;

/**
 * By default, only allows transformation of classes if they are loaded by the same class loader as this class.
 * <p/>
 * This avoids ClassNotFoundExceptions that can happen when instrumenting classes whose class loaders don't have
 * access to stagemonitor classes, for example the Profiler class.
 * <p/>
 * Also, this prevents to transform classes that are loaded by another class loader, for example when multiple
 * applications are deployed in a single Application Server.
 */
public class ApplicationClassLoaderMatcher extends ElementMatcher.Junction.AbstractBase<ClassLoader> {

	private static final boolean DEBUG_INSTRUMENTATION = Stagemonitor.getPlugin(CorePlugin.class).isDebugInstrumentation();

	private static final Logger logger = LoggerFactory.getLogger(ApplicationClassLoaderMatcher.class);

	@Override
	public boolean matches(ClassLoader target) {
		// only returns true if this class was loaded by the provided classLoader or by a parent of it
		// i.e. only if it is from the same application
		final boolean result = ClassUtils.loadClassOrReturnNull(target, "org.stagemonitor.core.Stagemonitor") == Stagemonitor.class;
		if (DEBUG_INSTRUMENTATION) {
			logger.info("Instrumenting ClassLoader {}: {}", target, result);
		}
		return result;
	}
}
