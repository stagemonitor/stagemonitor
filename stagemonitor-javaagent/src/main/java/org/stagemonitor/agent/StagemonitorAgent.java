package org.stagemonitor.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class StagemonitorAgent {

	public static void premain(String agentArgs, Instrumentation inst) {
		inst.addTransformer(new ClassFileTransformer() {

			private ClassFileTransformer mainStagemonitorClassFileTransformer;

			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
									ProtectionDomain protectionDomain, byte[] classfileBuffer)
					throws IllegalClassFormatException {
				if (loader == null) {
					return classfileBuffer;
				}
				if (mainStagemonitorClassFileTransformer == null) {
					initClassFileTransformer(loader);
				} else {
					try {
						return mainStagemonitorClassFileTransformer.transform(loader, className, classBeingRedefined,
								protectionDomain, classfileBuffer);
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
				return classfileBuffer;
			}

			@SuppressWarnings("unchecked")
			private void initClassFileTransformer(ClassLoader loader) {
				try {
					mainStagemonitorClassFileTransformer = (ClassFileTransformer) loader.loadClass("org.stagemonitor.core.instrument.MainStagemonitorClassFileTransformer").newInstance();
					// loader could load Stagemonitor - this is the application class loader
				} catch (Exception e) {
					// ignore; this is probably not the application class loader
				}
			}
		});
	}
}
