package org.stagemonitor.requestmonitor.freemarker;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import org.junit.Test;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class FreemarkerProfilingTest {

	@Test
	public void testFreemarkerProfiling() throws Exception {
		final CallStackElement callTree = Profiler.activateProfiling("testFreemarkerProfiling");
		final String renderedTemplate = processTemplate("test.ftl", "${templateModel.foo}", new TemplateModel());
		Profiler.stop();
		Profiler.deactivateProfiling();
		assertThat(renderedTemplate, is("foo"));
		System.out.println(callTree);

		assertThat(callTree.getChildren().size(), is(1));
		final CallStackElement freemarkerNode = callTree.getChildren().get(0);
		assertThat(freemarkerNode.getSignature(), is("test.ftl:1#templateModel.foo"));

		assertThat(freemarkerNode.getChildren().size(), is(1));
		final CallStackElement templateModelNode = freemarkerNode.getChildren().get(0);
		assertThat(templateModelNode.getSignature(), is("String org.stagemonitor.requestmonitor.freemarker.FreemarkerProfilingTest$TemplateModel.getFoo()"));
	}

	@Test
	public void testFreemarkerProfilingMethodCall() throws Exception {
		final CallStackElement callTree = Profiler.activateProfiling("testFreemarkerProfilingMethodCall");
		final String renderedTemplate = processTemplate("test.ftl", "${templateModel.getFoo()}", new TemplateModel());
		Profiler.stop();
		Profiler.deactivateProfiling();
		assertThat(renderedTemplate, is("foo"));
		System.out.println(callTree);

		assertThat(callTree.getChildren().size(), is(1));
		final CallStackElement freemarkerNode = callTree.getChildren().get(0);
		assertThat(freemarkerNode.getSignature(), is("test.ftl:1#templateModel.getFoo()"));

		assertThat(freemarkerNode.getChildren().size(), is(1));
		final CallStackElement templateModelNode = freemarkerNode.getChildren().get(0);
		assertThat(templateModelNode.getSignature(), is("String org.stagemonitor.requestmonitor.freemarker.FreemarkerProfilingTest$TemplateModel.getFoo()"));
	}

	@Test
	public void testShortSignature() {
		final String signature = "foobar.ftl:123#foo.getBar('123').baz";
		// don't try to shorten ftl signatures
		assertThat(CallStackElement.createRoot(signature).getShortSignature(), nullValue());
	}

	public static class TemplateModel {
		public String getFoo() {
			Profiler.start("String org.stagemonitor.requestmonitor.freemarker.FreemarkerProfilingTest$TemplateModel.getFoo()");
			try {
				return "foo";
			} finally {
				Profiler.stop();
			}
		}
	}

	private String processTemplate(String templateName, String templateString, TemplateModel templateModel) throws IOException, TemplateException {
		Template template = new Template(templateName, templateString, new Configuration(Configuration.VERSION_2_3_22));
		StringWriter out = new StringWriter(templateString.length());
		template.process(Collections.singletonMap("templateModel", templateModel), out);
		return out.toString();
	}

}
