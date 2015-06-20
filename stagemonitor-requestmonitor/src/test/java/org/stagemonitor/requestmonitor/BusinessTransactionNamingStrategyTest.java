package org.stagemonitor.requestmonitor;

import static org.stagemonitor.requestmonitor.BusinessTransactionNamingStrategy.CLASS_NAME_DOT_METHOD_NAME;
import static org.stagemonitor.requestmonitor.BusinessTransactionNamingStrategy.CLASS_NAME_HASH_METHOD_NAME;
import static org.stagemonitor.requestmonitor.BusinessTransactionNamingStrategy.METHOD_NAME_SPLIT_CAMEL_CASE;

import org.junit.Assert;
import org.junit.Test;

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
