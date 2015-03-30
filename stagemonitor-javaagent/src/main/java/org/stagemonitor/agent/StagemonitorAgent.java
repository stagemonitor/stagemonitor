package org.stagemonitor.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class StagemonitorAgent {

	private static ClassFileTransformer classFileTransformer;

	public static void premain(String agentArgs, Instrumentation inst) {
		inst.addTransformer(new ClassFileTransformer() {
			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
									ProtectionDomain protectionDomain, byte[] classfileBuffer)
					throws IllegalClassFormatException {
				if (loader == null) {
					return classfileBuffer;
				}
				try {
//					for (ClassFileTransformer classFileTransformer : ServiceLoader.load(StagemonitorClassFileTransformer.class, loader)) {
					if (classFileTransformer == null) {
						classFileTransformer = (ClassFileTransformer) loader.loadClass("org.stagemonitor.requestmonitor.profiler.ProfilingClassFileTransformer").newInstance();
					}
					classfileBuffer = classFileTransformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
//					}
				} catch (ClassNotFoundException e) {
					// ignore; this is probably not the application class loader
				} catch (NoClassDefFoundError e) {
					// ignore; this is probably not the application class loader
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return classfileBuffer;
			}
		});
	}

}