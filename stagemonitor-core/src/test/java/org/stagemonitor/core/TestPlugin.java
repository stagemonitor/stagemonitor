package org.stagemonitor.core;

import java.util.Collections;
import java.util.List;

public class TestPlugin extends StagemonitorPlugin {

	@Override
	public void initializePlugin(InitArguments initArguments) {
	}

	public List<Class<? extends StagemonitorPlugin>> dependsOn() {
		return Collections.emptyList();
	}

}
