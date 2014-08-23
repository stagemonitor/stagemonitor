package org.stagemonitor.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class IOUtils {

	private static final int EOF = -1;
	private static final int BUFFER_SIZE = 4096;

	public static void copy(InputStream input, OutputStream output) throws IOException {
		int n = 0;
		final byte[] buffer = new byte[BUFFER_SIZE];
		while (EOF != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
		}
	}

	public static void write(String str, OutputStream out) throws IOException {
		out.write(str.getBytes());
	}

	public static String toString(InputStream input) throws IOException {
		final InputStreamReader inputStreamReader = new InputStreamReader(input);
		final StringBuilder stringBuilder = new StringBuilder();
		final char[] buffer = new char[BUFFER_SIZE];
		int n = 0;
		while (EOF != (n = inputStreamReader.read(buffer))) {
			stringBuilder.append(buffer, 0, n);
		}
		return stringBuilder.toString();
	}
}
