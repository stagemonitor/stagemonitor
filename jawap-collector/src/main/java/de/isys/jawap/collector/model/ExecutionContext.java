package de.isys.jawap.collector.model;

/**
 * Represents a context of execution.
 * <p/>
 * A context might be a some sort of Request (HTTP, RMI, ...) or a background process
 */
public interface ExecutionContext {

	void setMethodCallStats(MethodCallStats methodCallStats);

}
