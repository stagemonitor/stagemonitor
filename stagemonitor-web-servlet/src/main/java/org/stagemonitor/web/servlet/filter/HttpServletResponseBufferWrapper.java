package org.stagemonitor.web.servlet.filter;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class HttpServletResponseBufferWrapper extends HttpServletResponseWrapper {

	private BufferingServletOutputStream servletOutputStream;
	private boolean usingOutputStream = false;

	private BufferingPrintWriter printWriter;
	private boolean usingWriter = false;
	private boolean committed = false;


	public HttpServletResponseBufferWrapper(HttpServletResponse response) {
		super(response);
	}

	@Override
	public BufferingPrintWriter getWriter() {
		if (usingOutputStream) {
			throw new IllegalStateException("getOutputStream has already been called");
		}
		usingWriter = true;
		if (printWriter == null) {
			printWriter = new BufferingPrintWriter();
		}
		return printWriter;
	}

	@Override
	public BufferingServletOutputStream getOutputStream() throws IOException {
		if (usingWriter) {
			throw new IllegalStateException("getWriter has already been called");
		}
		usingOutputStream = true;
		if (servletOutputStream == null) {
			servletOutputStream = new BufferingServletOutputStream();
		}
		return servletOutputStream;
	}

	@Override
	public void setContentLength(int len) {
		// ignore
	}

	@Override
	public void flushBuffer() throws IOException {
		// the purpose of this wrapper is to buffer all the content
		committed = true;
	}

	@Override
	public void setBufferSize(int size) {
		// ignore
	}

	@Override
	public void resetBuffer() {
		assertNotCommitted();
		servletOutputStream = null;
		usingOutputStream = false;
		printWriter = null;
		usingWriter = false;
	}

	@Override
	public void reset() {
		resetBuffer();
		super.reset();
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
		assertNotCommitted();
		super.sendError(sc, msg);
		committed = true;
	}

	@Override
	public void sendError(int sc) throws IOException {
		assertNotCommitted();
		super.sendError(sc);
		committed = true;
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		assertNotCommitted();
		super.sendRedirect(location);
		committed = true;
	}

	@Override
	public void setStatus(int sc, String sm) {
		if (!isCommitted()) {
			super.setStatus(sc, sm);
		}
	}

	@Override
	public void setStatus(int sc) {
		if (!isCommitted()) {
			super.setStatus(sc);
		}
	}

	@Override
	public boolean isCommitted() {
		return committed || super.isCommitted();
	}

	public boolean isUsingWriter() {
		return usingWriter;
	}

	public static class BufferingServletOutputStream extends ServletOutputStream {
		private ByteArrayOutputStream output = new ByteArrayOutputStream();
		@Override
		public void write(int b) throws IOException {
			output.write(b);
		}
		public ByteArrayOutputStream getOutput() {
			return output;
		}
	}

	public static class BufferingPrintWriter extends PrintWriter {
		private CharArrayWriter output;
		private BufferingPrintWriter() {
			this(new CharArrayWriter());
		}
		private BufferingPrintWriter(CharArrayWriter out) {
			super(out);
			this.output = out;
		}

		public CharArrayWriter getOutput() {
			return output;
		}
	}

	private void assertNotCommitted() {
		if (isCommitted()) {
			throw new IllegalStateException("The response has already been committed");
		}
	}

}
