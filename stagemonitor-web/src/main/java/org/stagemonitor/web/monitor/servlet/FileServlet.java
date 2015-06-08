package org.stagemonitor.web.monitor.servlet;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.stagemonitor.core.util.IOUtils;
import org.stagemonitor.core.util.StringUtils;

public class FileServlet extends HttpServlet {

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		String requestURI = req.getRequestURI().substring(req.getContextPath().length()).replace("..", "");
		res.setContentType(getMimeType(requestURI));
		InputStream inputStream = null;
		try {
			inputStream = getClass().getClassLoader().getResourceAsStream(StringUtils.removeStart(requestURI, "/"));
			if (inputStream == null) {
				res.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
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

	private String getMimeType(String path) {
		String mimeType = getServletContext().getMimeType(path);
		if (mimeType == null) {
			mimeType = "application/octet-stream";
		}
		return mimeType;
	}
}
