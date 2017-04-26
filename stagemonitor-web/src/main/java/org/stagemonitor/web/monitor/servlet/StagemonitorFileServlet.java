package org.stagemonitor.web.monitor.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.stagemonitor.util.IOUtils;
import org.stagemonitor.util.StringUtils;
import org.stagemonitor.web.monitor.rum.BoomerangJsHtmlInjector;

public class StagemonitorFileServlet extends HttpServlet {

	private final List<String> filesToCacheForever;

	public StagemonitorFileServlet() {
		this(Collections.singletonList(BoomerangJsHtmlInjector.BOOMERANG_FILENAME));
	}

	public StagemonitorFileServlet(List<String> filesToCacheForever) {
		this.filesToCacheForever = filesToCacheForever;
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		String requestURI = req.getRequestURI().substring(req.getContextPath().length()).replace("..", "");
		setResponseHeaders(req, res, requestURI);

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

	private void setResponseHeaders(HttpServletRequest req, HttpServletResponse res, String requestURI) {
		res.setContentType(getMimeType(requestURI));
		for (String file : filesToCacheForever) {
			if (req.getRequestURI().endsWith(file)) {
				res.setHeader("cache-control", "public, max-age=315360000");
				break;
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
