package org.stagemonitor.web.monitor.servlet;

import org.stagemonitor.core.util.IOUtils;
import org.stagemonitor.core.util.StringUtils;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FileServlet implements Servlet {
	private static final Map<String, String> mapExtensionToMimeType;
	static {
		mapExtensionToMimeType = new HashMap<String, String>();
		mapExtensionToMimeType.put(".css", "text/css");
		mapExtensionToMimeType.put(".js", "text/javascript");
		mapExtensionToMimeType.put(".png", "image/png");
		mapExtensionToMimeType.put(".html", "text/html");
	}

	private final String mimeType;
	private final String path;

	public FileServlet(String path) {
		this.mimeType = guessMimeType(path);
		this.path = StringUtils.removeStart(path, "/");
	}

	private String guessMimeType(String path) {
		final int lastDot = path.lastIndexOf(".");
		if(lastDot != -1) {
			final String probablyExtension = path.substring(lastDot);
			final String mimeTypeFound = mapExtensionToMimeType.get(probablyExtension);
			if(mimeTypeFound != null) {
				return mimeTypeFound;
			}
		}
		return "application/octet-stream"; // fallback
	}

	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		res.setContentType(mimeType);
		InputStream inputStream = null;
		try {
			inputStream = getClass().getClassLoader().getResourceAsStream(path);
			IOUtils.copy(inputStream, res.getOutputStream());
			res.getOutputStream().flush();
			res.flushBuffer();
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException ioe) {
				// ignore
			}
		}
	}

	@Override
	public void init(ServletConfig config) throws ServletException { }

	@Override
	public ServletConfig getServletConfig() {
		return null;
	}

	@Override
	public String getServletInfo() {
		return null;
	}

	@Override
	public void destroy() { }
}
