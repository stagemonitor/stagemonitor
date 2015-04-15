package org.stagemonitor.core.instrument;

import java.util.ArrayList;
import java.util.Collection;

import javassist.CtClass;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;

public abstract class StagemonitorJavassistInstrumenter {

	public static Collection<String> includes;

	public static Collection<String> excludes;

	public static Collection<String> excludeContaining;

	static {
		initIncludesAndExcludes();
	}

	private static void initIncludesAndExcludes() {
		CorePlugin requestMonitorPlugin = Stagemonitor.getConfiguration(CorePlugin.class);

		excludeContaining = new ArrayList<String>(requestMonitorPlugin.getExcludeContaining().size());
		for (String exclude : requestMonitorPlugin.getExcludeContaining()) {
			excludeContaining.add(exclude.replace('.', '/'));
		}

		excludes = new ArrayList<String>(requestMonitorPlugin.getExcludePackages().size());
		for (String exclude : requestMonitorPlugin.getExcludePackages()) {
			excludes.add(exclude.replace('.', '/'));
		}

		includes = new ArrayList<String>(requestMonitorPlugin.getIncludePackages().size());
		for (String include : requestMonitorPlugin.getIncludePackages()) {
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

	private boolean hasMoreSpecificExclude(String className, String include) {
		for (String exclude : excludes) {
			if (exclude.length() > include.length() && exclude.startsWith(include) && className.startsWith(exclude)) {
				return true;
			}
		}
		return false;
	}
}
