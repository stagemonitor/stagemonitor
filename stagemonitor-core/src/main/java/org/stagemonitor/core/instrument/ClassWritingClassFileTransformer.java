package org.stagemonitor.core.instrument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Collection;

public class ClassWritingClassFileTransformer implements ClassFileTransformer {
	private final ClassFileTransformer delegate;
	private final Collection<String> writeClasses;

	public ClassWritingClassFileTransformer(ClassFileTransformer delegate, Collection<String> writeClasses) {
		this.delegate = delegate;
		this.writeClasses = writeClasses;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		classfileBuffer = delegate.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
		if (writeClasses.contains(className)) {
			try {
				final File file = new File(System.getProperty("user.home") + "/classes/" + className.replace("/", ".") + ".class");
				file.delete();
				file.createNewFile();
				final FileOutputStream outputStream = new FileOutputStream(file);
				outputStream.write(classfileBuffer);
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return classfileBuffer;
	}
}
