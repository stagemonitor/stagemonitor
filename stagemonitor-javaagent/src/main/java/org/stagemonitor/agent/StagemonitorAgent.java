package org.stagemonitor.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ServiceLoader;

public class StagemonitorAgent {

	private static Instrumentation instrumentation;

	public static void premain(String agentArgs, final Instrumentation inst) {
		instrumentation = inst;
		inst.addTransformer(new ClassFileTransformer() {

			private boolean initialized = false;

			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
									ProtectionDomain protectionDomain, byte[] classfileBuffer)
					throws IllegalClassFormatException {
				if (loader == null) {
					return classfileBuffer;
				}
				if (!initialized) {
					initClassFileTransformer(loader);
				}
				return classfileBuffer;
			}

			@SuppressWarnings("unchecked")
			private void initClassFileTransformer(ClassLoader loader) {
				try {
					for (StagemonitorClassFileTransformer transformer : ServiceLoader.load(StagemonitorClassFileTransformer.class, loader)) {
						inst.addTransformer(transformer, true);
					}
					// loader could load StagemonitorClassFileTransformer - this is the application class loader
					initialized = true;
				} catch (Exception e) {
					// ignore; this is probably not the application class loader
				}
			}
		});
	}

	public static Instrumentation getInstrumentation() {
		return instrumentation;
	}
}
