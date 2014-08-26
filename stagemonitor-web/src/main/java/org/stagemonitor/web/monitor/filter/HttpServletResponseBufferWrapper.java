package org.stagemonitor.web.monitor.filter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class HttpServletResponseBufferWrapper extends HttpServletResponseWrapper {

	private BufferingServletOutputStream servletOutputStream;
	private boolean usingOutputStream = false;

	private BufferingPrintWriter printWriter;
	private boolean usingWriter = false;


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

}
