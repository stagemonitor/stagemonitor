package org.stagemonitor.web.monitor.filter;

import javax.servlet.ServletOutputStream;
import java.io.IOException;

public class CountingServletOutputStream extends ServletOutputStream {

	private ServletOutputStream wrappedServletOutputStream;
	private int bytesWritten = 0;

	public int getBytesWritten() {
		return bytesWritten;
	}

	public CountingServletOutputStream(ServletOutputStream wrappedServletOutputStream) {
		this.wrappedServletOutputStream = wrappedServletOutputStream;
	}

	public void write(int b) throws IOException {
		bytesWritten++;
		wrappedServletOutputStream.write(b);
	}

	public void write(byte[] b) throws IOException {
		bytesWritten += b.length;
		wrappedServletOutputStream.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		bytesWritten += len - off;
		wrappedServletOutputStream.write(b, off, len);
	}

	public void flush() throws IOException {
		wrappedServletOutputStream.flush();
	}

	public void close() throws IOException {
		wrappedServletOutputStream.close();
	}

	public void print(String s) throws IOException {
		wrappedServletOutputStream.print(s);
	}

	public void print(boolean b) throws IOException {
		wrappedServletOutputStream.print(b);
	}

	public void print(char c) throws IOException {
		wrappedServletOutputStream.print(c);
	}

	public void print(int i) throws IOException {
		wrappedServletOutputStream.print(i);
	}

	public void print(long l) throws IOException {
		wrappedServletOutputStream.print(l);
	}

	public void print(float f) throws IOException {
		wrappedServletOutputStream.print(f);
	}

	public void print(double d) throws IOException {
		wrappedServletOutputStream.print(d);
	}

	public void println() throws IOException {
		wrappedServletOutputStream.println();
	}

	public void println(String s) throws IOException {
		wrappedServletOutputStream.println(s);
	}

	public void println(boolean b) throws IOException {
		wrappedServletOutputStream.println(b);
	}

	public void println(char c) throws IOException {
		wrappedServletOutputStream.println(c);
	}

	public void println(int i) throws IOException {
		wrappedServletOutputStream.println(i);
	}

	public void println(long l) throws IOException {
		wrappedServletOutputStream.println(l);
	}

	public void println(float f) throws IOException {
		wrappedServletOutputStream.println(f);
	}

	public void println(double d) throws IOException {
		wrappedServletOutputStream.println(d);
	}
}
