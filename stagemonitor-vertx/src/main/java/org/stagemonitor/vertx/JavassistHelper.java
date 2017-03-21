package org.stagemonitor.vertx;

import javassist.*;

import java.io.IOException;

/**
 * Created by Glamarre on 3/17/2017.
 */
public class JavassistHelper {

	private static JavassistHelper instance;

	public static JavassistHelper getInstance(){
		if(instance == null){
			instance = new JavassistHelper();
		}
		return instance;
	}

	private ClassPool pool;

	private JavassistHelper(){
		pool = ClassPool.getDefault();
	}

	public byte[] processObservable(CtClass observable) throws NotFoundException, IOException, CannotCompileException {
		removeFinal(observable.getDeclaredMethod("subscribe", getClasses("rx.Subscriber")));
		setPublic(observable.getDeclaredField("onSubscribe"));
		return observable.toBytecode();
	}

	public void generateObservableSubClass() throws NotFoundException, CannotCompileException {
		//pool = ClassPool.getDefault();
		CtClass observable = getClass("rx.Observable");
		CtClass observableWrapper = pool.makeClass("org.stagemonitor.vertx.wrappers.ObservableWrapper", observable);
		observableWrapper.addField(CtField.make("private String behavior;", observableWrapper));

		CtConstructor constructor = CtNewConstructor.make("public ObservableWrapper(rx.Observable obs, java.lang.String behavior){super(obs.onSubscribe);}", observableWrapper);
		constructor.insertAfter("this.behavior = $2;");
		observableWrapper.addConstructor(constructor);

		CtMethod subscribe = CtNewMethod.delegator(observable.getDeclaredMethod("subscribe", getClasses("rx.Subscriber")), observableWrapper);
		subscribe.insertBefore("if(this.behavior.equals(\"MONITORING_MESSAGE\")){"
				+ "$1 = new com.netappsid.metrics.requestMonitor.RequestMonitoringSubscriber($1);"
				+ "} else if(this.behavior.equals(\"MONITORING_RESPONSE\")){"
				+ "$1 = new com.netappsid.metrics.requestMonitor.ResponseMonitoringSubscriber($1);}");
		observableWrapper.addMethod(subscribe);

		CtMethod getBehavior = CtNewMethod.make("public String getBehavior(){return this.behavior;}", observableWrapper);
		observableWrapper.addMethod(getBehavior);

		observableWrapper.toClass();
	}

	public CtClass getClass(String className) throws NotFoundException {
		return pool.get(className.replace('/','.'));
	}

	private CtClass[] getClasses(String...classNames) throws NotFoundException {
		CtClass[] classes = new CtClass[classNames.length];
		for (int i = 0; i < classNames.length; i++) {
			classes[i] = getClass(classNames[i]);
		}
		return classes;
	}

	private void removeFinal(CtMember member){
		int modifiers = member.getModifiers();
		if (Modifier.isFinal(modifiers)) {
			member.setModifiers(Modifier.clear(modifiers, Modifier.FINAL));
		}
	}

	private void setPublic(CtMember member){
		int modifiers = member.getModifiers();
		if (!Modifier.isPublic(modifiers)) {
			member.setModifiers(Modifier.setPublic(modifiers));
		}
	}
}
