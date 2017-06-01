package org.stagemonitor.core;

import java.util.Collections;
import java.util.List;

public class TestExceptionPlugin extends StagemonitorPlugin {

	@Override
	public void initializePlugin(InitArguments initArguments) {
		throw new RuntimeException("This is an expected test exception. " +
				"It is thrown to test whether Stagemonitor can cope with plugins that throw an exception.");
	}

	public List<Class<? extends StagemonitorPlugin>> dependsOn() {
		return Collections.emptyList();
	}

}
