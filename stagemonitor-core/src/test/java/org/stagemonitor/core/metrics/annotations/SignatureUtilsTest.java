package org.stagemonitor.core.metrics.annotations;

import org.junit.Assert;
import org.junit.Test;
import org.stagemonitor.core.metrics.aspects.SignatureUtils;

public class SignatureUtilsTest {

	@Test
	public void testGetSignature() throws Exception {
		Assert.assertEquals("toString", SignatureUtils.getSignature("String", "toString", null, true));
		Assert.assertEquals("String#toString", SignatureUtils.getSignature("java.lang.String", "toString", null, false));
		Assert.assertEquals("Enclosing$String#toString", SignatureUtils.getSignature("java.lang.Enclosing$String", "toString", null, false));
		Assert.assertEquals("String#toString", SignatureUtils.getSignature("String", "toString", null, false));
		Assert.assertEquals("stringify", SignatureUtils.getSignature("String", "toString", "stringify", true));
		Assert.assertEquals("String#stringify", SignatureUtils.getSignature("String", "toString", "stringify", false));
	}
}
