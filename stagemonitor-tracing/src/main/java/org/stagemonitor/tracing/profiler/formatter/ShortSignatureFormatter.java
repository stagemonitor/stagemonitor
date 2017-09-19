package org.stagemonitor.tracing.profiler.formatter;

import org.stagemonitor.tracing.profiler.CallStackElement;

public class ShortSignatureFormatter implements AsciiCallTreeSignatureFormatter {
	@Override
	public String getSignature(CallStackElement callTreeElement) {
		final String shortSignature = callTreeElement.getShortSignature();
		if (shortSignature != null) {
			return shortSignature;
		} else {
			return callTreeElement.getSignature();
		}
	}

	@Override
	public String toString() {
		return "Short Signature (no packages and parameters)";
	}
}
