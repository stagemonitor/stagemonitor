package org.stagemonitor.collector.web.monitor.filter;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

public class StatusExposingServletResponse extends HttpServletResponseWrapper {
	// The Servlet spec says: calling setStatus is optional, if no status is set, the default is 200.
	private int httpStatus = 200;

	public StatusExposingServletResponse(HttpServletResponse response) {
		super(response);
	}

	@Override
	public void sendError(int sc) throws IOException {
		httpStatus = sc;
		super.sendError(sc);
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
		httpStatus = sc;
		super.sendError(sc, msg);
	}

	@Override
	public void setStatus(int sc) {
		httpStatus = sc;
		super.setStatus(sc);
	}

	public int getStatus() {
		return httpStatus;
	}
}