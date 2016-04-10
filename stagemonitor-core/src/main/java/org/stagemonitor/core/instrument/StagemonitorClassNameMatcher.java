package org.stagemonitor.core.instrument;

import java.util.ArrayList;
import java.util.Collection;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;

/**
 * An {@link ElementMatcher} with the following logic:
 * <ul>
 * <li>Exclude all types which contain <code>stagemonitor.instrument.excludeContaining</code></li>
 * <li>Include all types <code>stagemonitor.instrument.include</code></li>
 * <li>If there are no more specific excludes in <code>stagemonitor.instrument.exclude</code></li>
 * </ul>
 */
public class StagemonitorClassNameMatcher extends ElementMatcher.Junction.AbstractBase<TypeDescription> {

	private static final Logger logger = LoggerFactory.getLogger(StagemonitorClassNameMatcher.class);

	private static Collection<String> includes;

	private static Collection<String> excludes;

	private static Collection<String> excludeContaining;


	static {
		initIncludesAndExcludes();
	}

	private static void initIncludesAndExcludes() {
		CorePlugin corePlugin = Stagemonitor.getPlugin(CorePlugin.class);

		excludeContaining = new ArrayList<String>(corePlugin.getExcludeContaining().size());
		for (String exclude : corePlugin.getExcludeContaining()) {
			excludeContaining.add(exclude);
		}

		excludes = new ArrayList<String>(corePlugin.getExcludePackages().size());
		excludes.add("org.stagemonitor");
		for (String exclude : corePlugin.getExcludePackages()) {
			excludes.add(exclude);
		}

		includes = new ArrayList<String>(corePlugin.getIncludePackages().size());
		for (String include : corePlugin.getIncludePackages()) {
			includes.add(include);
		}
		if (includes.isEmpty()) {
			logger.warn("No includes for instrumentation configured. Please set the stagemonitor.instrument.include property.");
		}
	}

	/**
	 * Checks if a specific class should be instrumented with this instrumenter
	 * <p/>
	 * The default implementation considers the following properties:
	 * <ul>
	 * <li><code>stagemonitor.instrument.excludeContaining</code></li>
	 * <li><code>stagemonitor.instrument.include</code></li>
	 * <li><code>stagemonitor.instrument.exclude</code></li>
	 * </ul>
	 *
	 * @param className The name of the class. For example java/lang/String
	 * @return <code>true</code>, if the class should be instrumented, <code>false</code> otherwise
	 */
	private boolean isIncluded(String className) {
		for (String exclude : excludeContaining) {
			if (className.contains(exclude)) {
				return false;
			}
		}

		for (String include : includes) {
			if (className.startsWith(include)) {
				return !hasMoreSpecificExclude(className, include);
			}
		}
		return false;
	}

	private boolean hasMoreSpecificExclude(String className, String include) {
		for (String exclude : excludes) {
			if (exclude.length() > include.length() && exclude.startsWith(include) && className.startsWith(exclude)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean matches(TypeDescription target) {
		return isIncluded(target.getName());
	}
}
