package de.isys.jawap.benchmark.aspectj;

public class InstrumentationTestObject {

	public long testMethod(long i) {
		AspectJAspect.dummy++;
		try {
			return ++i;
		} finally {
			AspectJAspect.dummy++;
		}
	}

	public long instrumentationAroundTestMethod(long i) {
		return ++i;
	}

	public long instrumentationBeforeAfterTestMethod(long i) {
		return ++i;
	}

}
