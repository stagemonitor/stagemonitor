package org.stagemonitor.tracing.profiler.formatter;

import org.stagemonitor.core.StagemonitorSPI;
import org.stagemonitor.tracing.profiler.CallStackElement;

public interface AsciiCallTreeSignatureFormatter extends StagemonitorSPI {

	String getSignature(CallStackElement callTreeElement);
}
