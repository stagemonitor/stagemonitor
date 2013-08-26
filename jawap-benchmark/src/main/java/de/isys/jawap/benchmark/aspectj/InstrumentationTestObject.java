package de.isys.jawap.benchmark.aspectj;

public class InstrumentationTestObject {

	public Long testMethod(Long i) {
		return ++i;
	}

	public Long instrumentationAroundTestMethod(Long i) {
		return ++i;
	}

	public Long instrumentationBeforeAfterTestMethod(Long i) {
		return ++i;
	}

}
