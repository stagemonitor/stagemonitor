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
}
