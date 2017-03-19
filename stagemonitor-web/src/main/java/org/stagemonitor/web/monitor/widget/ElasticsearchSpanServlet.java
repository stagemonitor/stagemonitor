package org.stagemonitor.web.monitor.widget;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.IOUtils;
import org.stagemonitor.core.util.StringUtils;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ElasticsearchSpanServlet extends HttpServlet {

	private final ElasticsearchClient elasticsearchClient;

	public ElasticsearchSpanServlet() {
		this(Stagemonitor.getConfiguration());
	}

	public ElasticsearchSpanServlet(Configuration configuration) {
		elasticsearchClient = configuration.getConfig(CorePlugin.class).getElasticsearchClient();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		final String spanId = req.getParameter("id");
		if (StringUtils.isEmpty(spanId)) {
			throw new IllegalArgumentException("Parameter id is missing");
		}

		resp.setContentType("application/json");
		resp.setHeader("Pragma", "no-cache");
		resp.setHeader("Cache-Control", "max-age=0, no-cache, no-store, must-revalidate");
		resp.setHeader("Expires", "0");
		resp.setCharacterEncoding("UTF-8");

		IOUtils.write(elasticsearchClient
				.getJson("/stagemonitor-spans-*/_search?q=id:" + spanId.replaceAll("[^a-zA-Z0-9\\-]", ""))
				.get("hits")
				.get("hits")
				.elements()
				.next()
				.get("_source")
				.toString(), resp.getOutputStream());
	}
}
