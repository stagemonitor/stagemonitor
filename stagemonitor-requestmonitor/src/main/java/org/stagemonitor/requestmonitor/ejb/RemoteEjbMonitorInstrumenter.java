package org.stagemonitor.requestmonitor.ejb;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Remote;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.requestmonitor.MonitorRequestsInstrumenter;

public class RemoteEjbMonitorInstrumenter extends StagemonitorJavassistInstrumenter {

	private static final Logger logger = LoggerFactory.getLogger(RemoteEjbMonitorInstrumenter.class);

	private final Class<?> remoteAnnotation;

	public RemoteEjbMonitorInstrumenter() {
		remoteAnnotation = ClassUtils.forNameOrNull("javax.ejb.Remote");
	}

	@Override
	public void transformClass(CtClass ctClass, ClassLoader loader) throws Exception {
		if (!ctClass.isInterface() && ctClass.hasAnnotation(remoteAnnotation)) {
			transformRemoteMethods(ctClass, (Remote) ctClass.getAnnotation(remoteAnnotation), ctClass.getClassPool());
		}
	}

	private void transformRemoteMethods(CtClass ctClass, Remote annotation, ClassPool classPool) throws NotFoundException {
		List<CtClass> remoteCtInterfaces = new ArrayList<CtClass>(annotation.value().length);
		for (Class remoteInterface : annotation.value()) {
			try {
				remoteCtInterfaces.add(classPool.get(remoteInterface.getName()));
			} catch (NotFoundException e) {
				logger.debug(e.getMessage(), e);
			}
		}

		for (CtMethod ctMethod : ctClass.getMethods()) {
			if (isDeclaredInRemoteInterface(ctMethod, remoteCtInterfaces)) {
				MonitorRequestsInstrumenter.monitorMethodCall(ctClass, ctMethod);
			}
		}
	}

	private boolean isDeclaredInRemoteInterface(CtMethod ctMethod, List<CtClass> remoteCtInterfaces) throws NotFoundException {
		CtClass object = ctMethod.getDeclaringClass().getClassPool().get(Object.class.getName());
		for (CtClass remoteCtInterface : remoteCtInterfaces) {
			if (!isMethodDeclaredInClass(ctMethod, object) && isMethodDeclaredInClass(ctMethod, remoteCtInterface)) {
				return true;
			}
		}
		return false;
	}

	private boolean isMethodDeclaredInClass(CtMethod ctMethod, CtClass remoteCtInterface) {
		try {
			remoteCtInterface.getMethod(ctMethod.getName(), ctMethod.getMethodInfo().getDescriptor());
			return true;
		} catch (NotFoundException e) {
			return false;
		}
	}

	@Override
	public boolean isIncluded(String className) {
		return remoteAnnotation != null && super.isIncluded(className);
	}
}
