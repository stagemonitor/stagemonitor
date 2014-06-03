package org.stagemonitor.collector.web.monitor.filter;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;

public class StatusExposingByteCountingServletResponse extends HttpServletResponseWrapper {
	// The Servlet spec says: calling setStatus is optional, if no status is set, the default is 200.
	private int httpStatus = 200;

	private CountingServletOutputStream servletOutputStreamWrapper;

	private PrintWriter printWriter;

	public StatusExposingByteCountingServletResponse(HttpServletResponse response) throws IOException {
		super(response);
		servletOutputStreamWrapper = new CountingServletOutputStream(response.getOutputStream());
		printWriter = new PrintWriter(servletOutputStreamWrapper);
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

	@Override
	public CountingServletOutputStream getOutputStream() throws IOException {
		return servletOutputStreamWrapper;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return printWriter;
	}
}