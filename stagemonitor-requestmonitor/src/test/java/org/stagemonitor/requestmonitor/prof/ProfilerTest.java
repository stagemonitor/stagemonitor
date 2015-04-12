package org.stagemonitor.requestmonitor.prof;

import org.junit.Assert;
import org.junit.Test;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;

public class ProfilerTest {

	@Test
	public void testProfiler() {
		ProfilerTest profilerTest = new ProfilerTest();
		CallStackElement total = Profiler.activateProfiling("total");
		profilerTest.method1();
		Profiler.stop();

		Assert.assertEquals(total.toString(), 1, total.getChildren().size());
		Assert.assertEquals(total.toString(), 3, total.getChildren().get(0).getChildren().size());
		Assert.assertEquals(total.toString(), "int org.stagemonitor.requestmonitor.prof.ProfilerTest.method5()",
				total.getChildren().get(0).getChildren().get(2).getSignature());
	}

	public int method1() {
		return method2(1) + method3() + method5();
	}

	private int method2(int i) {
		return 1 + i;
	}

	private int method3() {
		return method4();
	}

	private int method4() {
		return 4;
	}

	private int method5() {
		return method6() + method7();
	}

	private int method6() {
		int value = 1;
		if (Math.random() > 0.5) {
			return 6;
		}
		switch (value) {
			case 1:
				value = 6;
				break;
		}
		return value;
	}

	private int method7() {
		return method8();
	}

	private int method8() {
		return method9();
	}

	private int method9() {
		return 9;
	}

}
