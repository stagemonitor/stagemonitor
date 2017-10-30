package org.stagemonitor.web.servlet.initializer;

import org.stagemonitor.core.StagemonitorSPI;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Implement this interface and register it as described in {@link StagemonitorSPI} in order to automatically register
 * servlets and filters
 *
 * <p> This interface is needed as not all servlet containers (especially embedded ones like used in spring boot) do not
 * support {@link javax.servlet.ServletContainerInitializer} </p>
 */
public interface StagemonitorServletContainerInitializer extends StagemonitorSPI {

	void onStartup(ServletContext ctx) throws ServletException;
}
