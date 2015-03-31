package org.stagemonitor.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ServiceLoader;

public class StagemonitorAgent {

	public static void premain(String agentArgs, Instrumentation inst) {
		inst.addTransformer(new ClassFileTransformer() {

			private Iterable<? extends ClassFileTransformer> classFileTransformers;

			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
									ProtectionDomain protectionDomain, byte[] classfileBuffer)
					throws IllegalClassFormatException {
				if (loader == null) {
					return classfileBuffer;
				}
				initClassFileTransformers(loader);
				if (classFileTransformers != null) {
					for (ClassFileTransformer classFileTransformer : classFileTransformers) {
						classfileBuffer = classFileTransformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
					}
				}
				return classfileBuffer;
			}

			@SuppressWarnings("unchecked")
			private void initClassFileTransformers(ClassLoader loader) {
				if (classFileTransformers == null) {
					try {
						Class<?> classFileTransformerClass = loader.loadClass("org.stagemonitor.core.instrument.StagemonitorClassFileTransformer");
						classFileTransformers = (Iterable<? extends ClassFileTransformer>) ServiceLoader.load(classFileTransformerClass, loader);
						for (ClassFileTransformer classFileTransformer : classFileTransformers) {
							System.out.println("Registering " + classFileTransformer.getClass().getSimpleName());
						}
					} catch (ClassNotFoundException e) {
						// ignore; this is probably not the application class loader
					}
				}
			}
		});
	}
}
