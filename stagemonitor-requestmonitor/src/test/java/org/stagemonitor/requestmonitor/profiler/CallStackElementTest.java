package org.stagemonitor.requestmonitor.profiler;

import org.junit.Assert;
import org.junit.Test;

public class CallStackElementTest {

	@Test
	public void printPercentAsBar() throws Exception {
		Assert.assertEquals("|||||-----", CallStackElement.printPercentAsBar(0.54d, 10, false));
		// █████▓▒▒▒▒
		char[] chars = {9608, 9608, 9608, 9608, 9608, 9619, 9617, 9617, 9617, 9617};
		Assert.assertEquals(new String(chars), CallStackElement.printPercentAsBar(0.56d, 10, true));
	}

	@Test
	public void testGetShortSignature() {
		CallStackElement callStackElement = CallStackElement.createRoot("public void org.stagemonitor.requestmonitor.profiler.CallStackElementTest.testGetShortSignature()");
		Assert.assertEquals("CallStackElementTest#testGetShortSignature", callStackElement.getShortSignature());
	}

	@Test
	public void testGetShortSignatureTotal() {
		CallStackElement callStackElement = CallStackElement.createRoot("total");
		Assert.assertNull(callStackElement.getShortSignature());
	}
}
