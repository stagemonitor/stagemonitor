package org.stagemonitor.tracing.profiler.formatter;

import org.stagemonitor.tracing.profiler.CallStackElement;

public class LongMethodNameFormatter implements AsciiCallTreeSignatureFormatter {
	@Override
	public String getSignature(CallStackElement callTreeElement) {
		return callTreeElement.getSignature();
	}

	@Override
	public String toString() {
		return "Long Signature (full package names and parameters)";
	}
}
