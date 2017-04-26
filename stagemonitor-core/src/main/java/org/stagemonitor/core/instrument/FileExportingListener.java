package org.stagemonitor.core.instrument;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class FileExportingListener extends AgentBuilder.Listener.Adapter {

	private static final Logger logger = LoggerFactory.getLogger(FileExportingListener.class);

	private final Collection<String> exportClassesWithName;
	static final List<String> exportedClasses = new ArrayList<String>();

	FileExportingListener(Collection<String> exportClassesWithName) {
		this.exportClassesWithName = exportClassesWithName;
	}

	@Override
	public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
		if (!exportClassesWithName.contains(typeDescription.getName())) {
			return;
		}
		final File exportedClass;
		try {
			exportedClass = File.createTempFile(typeDescription.getName(), ".class");
			IOUtils.copy(new ByteArrayInputStream(dynamicType.getBytes()), new FileOutputStream(exportedClass));
			logger.info("Exported class modified by Byte Buddy: {}", exportedClass.getAbsolutePath());
			exportedClasses.add(exportedClass.getAbsolutePath());
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
	}
}
