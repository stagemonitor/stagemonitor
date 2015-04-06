package org.stagemonitor.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;

public class StagemonitorAgent {

	private static Instrumentation instrumentation;

	public static void premain(String agentArgs, final Instrumentation inst) {
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
					final ClassFileTransformer mainStagemonitorClassFileTransformer = (ClassFileTransformer) loader
							.loadClass("org.stagemonitor.core.instrument.MainStagemonitorClassFileTransformer")
							.newInstance();
					inst.addTransformer(mainStagemonitorClassFileTransformer, true);
					// loader could load MainStagemonitorClassFileTransformer - this is the application class loader
					instrumentation = inst;
					initialized = true;
				} catch (Exception e) {
					// ignore; this is probably not the application class loader
				}
			}
		});
	}

	public static Instrumentation getInstrumentation() {
		try {
			// instrumentation can't be accessed directly due to classloader issues
			Field field = ClassLoader.getSystemClassLoader()
					.loadClass(StagemonitorAgent.class.getName())
					.getDeclaredField("instrumentation");
			field.setAccessible(true);
			return (Instrumentation) field.get(null);
		} catch (Exception e) {
			throw new IllegalStateException("The is not properly initialized", e);
		}
	}
}
