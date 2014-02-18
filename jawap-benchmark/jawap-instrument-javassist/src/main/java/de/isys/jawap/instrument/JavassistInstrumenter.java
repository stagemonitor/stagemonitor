package de.isys.jawap.instrument;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class JavassistInstrumenter implements ClassFileTransformer {

	public static void premain(String agentArgs, Instrumentation inst) {
		inst.addTransformer(new JavassistInstrumenter());
	}

	public byte[] transform(ClassLoader loader, String className, Class classBeingRedefined,
							ProtectionDomain protectionDomain, byte[] classfileBuffer)
			throws IllegalClassFormatException {

		byte[] byteCode = classfileBuffer;

		if (className.equals("de/isys/jawap/benchmark/profiler/ClassJavassistProfiled")) {

			try {
				ClassPool cp = ClassPool.getDefault();
				CtClass cc = cp.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));
				for (CtMethod m : cc.getDeclaredMethods()) {
					m.insertBefore("de.isys.jawap.collector.profiler.Profiler.start();");
					m.insertAfter("de.isys.jawap.collector.profiler.Profiler.stop(\""+m.getLongName()+"\");", true);
				}
				byteCode = cc.toBytecode();
				cc.detach();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		return byteCode;
	}
}