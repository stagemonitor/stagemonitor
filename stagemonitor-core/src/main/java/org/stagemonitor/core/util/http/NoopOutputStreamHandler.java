package org.stagemonitor.core.util.http;

import org.stagemonitor.core.util.HttpClient.OutputStreamHandler;

import java.io.IOException;
import java.io.OutputStream;

public class NoopOutputStreamHandler implements OutputStreamHandler {

	public static final NoopOutputStreamHandler INSTANCE = new NoopOutputStreamHandler();

	NoopOutputStreamHandler() {
	}

	@Override
	public void withHttpURLConnection(OutputStream os) throws IOException {
		os.close();
	}
}
