package org.stagemonitor.tracing;

import org.junit.Assert;
import org.junit.Test;

import static org.stagemonitor.tracing.BusinessTransactionNamingStrategy.CLASS_NAME_DOT_METHOD_NAME;
import static org.stagemonitor.tracing.BusinessTransactionNamingStrategy.CLASS_NAME_HASH_METHOD_NAME;
import static org.stagemonitor.tracing.BusinessTransactionNamingStrategy.METHOD_NAME_SPLIT_CAMEL_CASE;

public class BusinessTransactionNamingStrategyTest {

	@Test
	public void testGetBusinessTransationName() throws Exception {
		Assert.assertEquals("Say Hello", METHOD_NAME_SPLIT_CAMEL_CASE
				.getBusinessTransationName("HelloController", "sayHello"));

		Assert.assertEquals("HelloController.sayHello", CLASS_NAME_DOT_METHOD_NAME
				.getBusinessTransationName("HelloController", "sayHello"));

		Assert.assertEquals("HelloController#sayHello", CLASS_NAME_HASH_METHOD_NAME
				.getBusinessTransationName("HelloController", "sayHello"));
	}
}
