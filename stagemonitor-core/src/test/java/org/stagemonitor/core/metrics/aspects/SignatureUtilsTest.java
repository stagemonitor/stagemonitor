package org.stagemonitor.core.metrics.aspects;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.junit.Assert;
import org.junit.Test;

public class SignatureUtilsTest {

	@Test
	public void testGetSignature() throws Exception {
		CtClass ctClass = ClassPool.getDefault().get("java.lang.String");
		CtMethod toString = ctClass.getDeclaredMethod("toString");
		Assert.assertEquals("toString", SignatureUtils.getSignature(toString, null, true));
		Assert.assertEquals("String#toString", SignatureUtils.getSignature(toString, null, false));
		Assert.assertEquals("stringify", SignatureUtils.getSignature(toString, "stringify", true));
		Assert.assertEquals("String#stringify", SignatureUtils.getSignature(toString, "stringify", false));
	}
}
