package org.stagemonitor.core.instrument;

import java.util.ArrayList;
import java.util.Collection;

import javassist.CtClass;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.ClassUtils;

public abstract class StagemonitorJavassistInstrumenter {

	public static Collection<String> includes;

	public static Collection<String> excludes;

	public static Collection<String> excludeContaining;

	static {
		initIncludesAndExcludes();
	}

	private static void initIncludesAndExcludes() {
		CorePlugin corePlugin = Stagemonitor.getConfiguration(CorePlugin.class);

		excludeContaining = new ArrayList<String>(corePlugin.getExcludeContaining().size());
		for (String exclude : corePlugin.getExcludeContaining()) {
			excludeContaining.add(exclude.replace('.', '/'));
		}

		excludes = new ArrayList<String>(corePlugin.getExcludePackages().size());
		for (String exclude : corePlugin.getExcludePackages()) {
			excludes.add(exclude.replace('.', '/'));
		}

		includes = new ArrayList<String>(corePlugin.getIncludePackages().size());
		for (String include : corePlugin.getIncludePackages()) {
			includes.add(include.replace('.', '/'));
		}
	}

	public void transformClass(CtClass ctClass, ClassLoader loader) throws Exception {
	}

	/**
	 * Checks if a specific class should be instrumented with this instrumenter
	 * <p/>
	 * The default implementation considers the following properties:
	 * <ul>
	 *     <li><code>stagemonitor.instrument.excludeContaining</code></li>
	 *     <li><code>stagemonitor.instrument.include</code></li>
	 *     <li><code>stagemonitor.instrument.exclude</code></li>
	 * </ul>
	 *
	 * @param className The name of the class. For example java/lang/String
	 * @return <code>true</code>, if the class should be instrumented, <code>false</code> otherwise
	 */
	public boolean isIncluded(String className) {
		for (String exclude : excludeContaining) {
			if (className.contains(exclude)) {
				return false;
			}
		}

		// no includes -> include all
		boolean instrument = includes.isEmpty();
		for (String include : includes) {
			if (className.startsWith(include)) {
				return !hasMoreSpecificExclude(className, include);
			}
		}
		if (!instrument) {
			return false;
		}
		for (String exclude : excludes) {
			if (className.startsWith(exclude)) {
				return false;
			}
		}
		return instrument;
	}

	/**
	 * By default, only allows transformation of classes if they are loaded by the same class loader as this class.
	 * <p/>
	 * This avoids ClassNotFoundExceptions that can happen when instrumenting classes whose class loaders don't have
	 * access to stagemonitor classes, for example the Profiler class.
	 * <p/>
	 * Also, this prevents to transform classes that are loaded by another class loader, for example when multiple
	 * applications are deployed in a single Application Server.
	 */
	public boolean isTransformClassesOfClassLoader(ClassLoader classLoader) {
		// only returns true if this class was loaded by the provided classLoader or by a parent of it
		// i.e. only if it is from the same application
		return ClassUtils.loadClassOrReturnNull(classLoader, getClass().getName()) == getClass();
	}

	private boolean hasMoreSpecificExclude(String className, String include) {
		for (String exclude : excludes) {
			if (exclude.length() > include.length() && exclude.startsWith(include) && className.startsWith(exclude)) {
				return true;
			}
		}
		return false;
	}
}
