package org.stagemonitor.tracing.sampling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.tracing.wrapper.StatelessSpanEventListener;

import java.util.Collection;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

import io.opentracing.tag.Tags;

public class SamplePriorityDeterminingSpanEventListener extends StatelessSpanEventListener {

	private static final Logger logger = LoggerFactory.getLogger(SamplePriorityDeterminingSpanEventListener.class);
	private final Collection<PreExecutionSpanInterceptor> preInterceptors =
			new CopyOnWriteArrayList<PreExecutionSpanInterceptor>();
	private final Collection<PostExecutionSpanInterceptor> postInterceptors =
			new CopyOnWriteArrayList<PostExecutionSpanInterceptor>();
	private final ConfigurationRegistry configuration;
	private final TracingPlugin tracingPlugin;

	public SamplePriorityDeterminingSpanEventListener(ConfigurationRegistry configuration) {
		this(configuration,
				ServiceLoader.load(PreExecutionSpanInterceptor.class, SamplePriorityDeterminingSpanEventListener.class.getClassLoader()),
				ServiceLoader.load(PostExecutionSpanInterceptor.class, SamplePriorityDeterminingSpanEventListener.class.getClassLoader()));
	}

	public SamplePriorityDeterminingSpanEventListener(ConfigurationRegistry configuration, Iterable<PreExecutionSpanInterceptor> preExecutionSpanInterceptors, Iterable<PostExecutionSpanInterceptor> postExecutionSpanInterceptors) {
		this.configuration = configuration;
		this.tracingPlugin = configuration.getConfig(TracingPlugin.class);
		registerPreInterceptors(preExecutionSpanInterceptors);
		registerPostInterceptors(postExecutionSpanInterceptors);
	}

	private void registerPreInterceptors(Iterable<PreExecutionSpanInterceptor> preExecutionSpanInterceptors) {
		for (PreExecutionSpanInterceptor interceptor : preExecutionSpanInterceptors) {
			addPreInterceptor(interceptor);
		}
	}

	private void registerPostInterceptors(Iterable<PostExecutionSpanInterceptor> postExecutionSpanInterceptors) {
		for (PostExecutionSpanInterceptor interceptor : postExecutionSpanInterceptors) {
			addPostInterceptor(interceptor);
		}
	}

	@Override
	public void onStart(SpanWrapper spanWrapper) {
		final SpanContextInformation spanContext = SpanContextInformation.forSpan(spanWrapper);
		if (!tracingPlugin.isSampled(spanWrapper)) {
			return;
		}

		PreExecutionInterceptorContext context = new PreExecutionInterceptorContext(spanContext);
		for (PreExecutionSpanInterceptor interceptor : preInterceptors) {
			try {
				interceptor.interceptReport(context);
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}

		spanContext.setPreExecutionInterceptorContext(context);

		if (!context.isReport()) {
			Tags.SAMPLING_PRIORITY.set(spanWrapper, 0);
		}
	}

	@Override
	public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
		final SpanContextInformation info = SpanContextInformation.forSpan(spanWrapper);
		if (!tracingPlugin.isSampled(spanWrapper)) {
			return;
		}
		PostExecutionInterceptorContext context = new PostExecutionInterceptorContext(info);
		for (PostExecutionSpanInterceptor interceptor : postInterceptors) {
			try {
				interceptor.interceptReport(context);
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
		info.setPostExecutionInterceptorContext(context);
		if (!context.isReport()) {
			Tags.SAMPLING_PRIORITY.set(spanWrapper, 0);
		}
	}

	public void addPreInterceptor(PreExecutionSpanInterceptor interceptor) {
		interceptor.init(configuration);
		preInterceptors.add(interceptor);
	}

	public void addPostInterceptor(PostExecutionSpanInterceptor interceptor) {
		interceptor.init(configuration);
		postInterceptors.add(interceptor);
	}
}
