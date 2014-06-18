package org.stagemonitor.collector.web.monitor.filter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

public class StatusExposingByteCountingServletResponse extends HttpServletResponseWrapper {
	// The Servlet spec says: calling setStatus is optional, if no status is set, the default is 200.
	private int httpStatus = 200;

	private CountingServletOutputStream servletOutputStreamWrapper;
	private int contentLength;

	public StatusExposingByteCountingServletResponse(HttpServletResponse response) throws IOException {
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

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		final ServletOutputStream outputStream = super.getOutputStream();
		if (servletOutputStreamWrapper == null) {
			servletOutputStreamWrapper = new CountingServletOutputStream(outputStream);

		}
		return servletOutputStreamWrapper;
	}

	@Override
	public void setContentLength(int len) {
		super.setContentLength(len);
		this.contentLength = len;
	}

	public Integer getContentLength() {
		if (contentLength > 0) {
			return contentLength;
		} else if (servletOutputStreamWrapper != null) {
			return servletOutputStreamWrapper.getBytesWritten();
		} else {
			return null;
		}
	}
}