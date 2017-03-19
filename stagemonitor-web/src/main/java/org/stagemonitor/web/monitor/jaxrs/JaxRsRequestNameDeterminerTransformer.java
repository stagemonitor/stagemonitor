package org.stagemonitor.web.monitor.jaxrs;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.requestmonitor.AbstractMonitorRequestsTransformer;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

public class JaxRsRequestNameDeterminerTransformer extends StagemonitorByteBuddyTransformer {

	@Override
	protected ElementMatcher.Junction<TypeDescription> getNarrowTypesMatcher() {
		return isAnnotatedWith(Path.class);
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return isAnnotatedWith(GET.class)
				.or(isAnnotatedWith(POST.class))
				.or(isAnnotatedWith(PUT.class))
				.or(isAnnotatedWith(DELETE.class))
				.or(isAnnotatedWith(HEAD.class));
	}

	@Override
	protected List<StagemonitorDynamicValue<?>> getDynamicValues() {
		return Collections.<StagemonitorDynamicValue<?>>singletonList(new AbstractMonitorRequestsTransformer.RequestNameDynamicValue());
	}

	@Override
	public boolean isActive() {
		return ClassUtils.isPresent("javax.ws.rs.Path");
	}

	@Advice.OnMethodEnter(inline = false)
	public static void setRequestName(@AbstractMonitorRequestsTransformer.RequestName String requestName) {
		RequestMonitorPlugin.getSpan().setOperationName(requestName);
	}
}
