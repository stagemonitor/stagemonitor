package org.stagemonitor.web.monitor.filter;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class HttpServletResponseBufferWrapper extends HttpServletResponseWrapper {

	private static final int INITIAL_SIZE = 50000;
	private static final int BUFFER_SIZE = 1024*1024*1;

	private CharArrayWriter output;

	public HttpServletResponseBufferWrapper(HttpServletResponse response) {
		super(response);
		response.setBufferSize(BUFFER_SIZE);
		resetBuffer();
	}

	public String getBufferedContent() {
		return output.toString();
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return new PrintWriter(output);
	}

	@Override
	public void flushBuffer() throws IOException {
		// ignore
	}

	@Override
	public void setBufferSize(int size) {
		// ignore
	}

	@Override
	public void resetBuffer() {
		output = new CharArrayWriter(INITIAL_SIZE);
	}
}
