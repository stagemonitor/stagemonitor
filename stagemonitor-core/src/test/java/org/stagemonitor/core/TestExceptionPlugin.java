package org.stagemonitor.core;

public class TestExceptionPlugin extends StagemonitorPlugin {

	@Override
	public void initializePlugin(InitArguments initArguments) {
		throw new RuntimeException("This is an expected test exception. " +
				"It is thrown to test whether Stagemonitor can cope with plugins that throw an exception.");
	}

}
