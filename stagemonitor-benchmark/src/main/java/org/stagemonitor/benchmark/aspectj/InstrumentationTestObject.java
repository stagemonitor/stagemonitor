package org.stagemonitor.benchmark.aspectj;

public class InstrumentationTestObject {

	public long noAspectObjectLong(Long i) {
		AspectJAspect.dummy++;
		try {
			return ++i;
		} finally {
			AspectJAspect.dummy++;
		}
	}
	public long noAspectPrimitiveLong(long i) {
		AspectJAspect.dummy++;
		try {
			return ++i;
		} finally {
			AspectJAspect.dummy++;
		}
	}

	public long aroundPrimitiveLong(long i) {
		return ++i;
	}

	public long aroundObjectLong(long i) {
		return ++i;
	}

	public long beforeAfterObjectLong(Long i) {
		return ++i;
	}

	public long beforeAfterPrimitiveLong(long i) {
		return ++i;
	}

}
