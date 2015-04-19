package org.stagemonitor.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class StagemonitorAgent {

	private static final String INSTRUMENTATION_KEY = StagemonitorAgent.class + ".instrumentation";

	private static boolean initialized = false;

	/**
	 * Allows the installation of the agent via the -javaagent command line argument
	 *
	 * @param agentArgs the agent arguments
	 * @param inst      the instrumentation
	 */
	public static void premain(String agentArgs, final Instrumentation inst) {
		inst.addTransformer(new ClassFileTransformer() {

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
					final ClassFileTransformer mainStagemonitorClassFileTransformer = getMainStagemonitorClassFileTransformer(loader, inst);
					inst.addTransformer(mainStagemonitorClassFileTransformer, true);
					initialized = true;
					// loader could load MainStagemonitorClassFileTransformer - this is the application class loader
				} catch (Exception e) {
					// ignore; this is probably not the application class loader
				}
			}
		});
	}

	private static ClassFileTransformer getMainStagemonitorClassFileTransformer(ClassLoader loader, Instrumentation inst)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		final Class<?> clazz = loader.loadClass("org.stagemonitor.core.instrument.MainStagemonitorClassFileTransformer");
		System.getProperties().put(INSTRUMENTATION_KEY, inst);
		return (ClassFileTransformer) clazz.newInstance();
	}

	/**
	 * Allows the runtime installation of this agent via the Attach API
	 *
	 * @param args            the agent arguments
	 * @param instrumentation the instrumentation
	 */
	public static void agentmain(String args, Instrumentation instrumentation) {
		System.getProperties().put(INSTRUMENTATION_KEY, instrumentation);
	}

	public static Instrumentation getInstrumentation() {
		try {
			return (Instrumentation) System.getProperties().get(INSTRUMENTATION_KEY);
		} catch (Exception e) {
			throw new IllegalStateException("The agent is not properly initialized", e);
		}
	}

}
