package org.stagemonitor.core;

/**
 * Just marks a class as a serivice provider interface that can be used to extend stagemonitor.
 * <p/>
 * To add a custom implementation, just implement the abstract class and add the full qualified class name to
 * <code>src/main/resources/META-INF/services/{full.class.name.of.SPI}</code>
 * (for example <code>src/main/resources/META-INF/services/org.stagemonitor.core.StagemonitorPlugin</code>).
 * <p/>
 * The content of the file has to be the canonical class name of the implementation.
 */
public interface StagemonitorSPI {
}
